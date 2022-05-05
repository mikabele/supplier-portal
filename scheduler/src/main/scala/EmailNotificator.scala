import cats.effect.{Async, Blocker, ContextShift, ExitCode, IO, IOApp, Resource, Sync, Timer}
import cats.implicits.catsSyntaxApplicativeId
import conf.app.AppConf
import conf.db.{migrator, transactor}
import io.circe.config.parser
import io.circe.generic.auto._
import kafka.KafkaConsumerService
import logger.LogHandler
import org.apache.logging.log4j.LogManager
import pureconfig.ConfigSource
import repository.{ProductGroupRepository, ProductRepository, SubscriptionRepository, UserRepository}
import service.EmailNotificationService
import util.KafkaSerializationUtil.kafkaDeserializer

import java.util.UUID
import scala.jdk.CollectionConverters._

object EmailNotificator extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    notificatorResource[IO]().use(n => n.start()).as(ExitCode.Success)
  }

  def notificatorResource[F[+_]: Async: Timer: ContextShift](
  ): Resource[F, EmailNotificationService[F]] = {

    for {
      conf <- Resource.eval(parser.decodePathF[F, AppConf]("app"))
      be   <- Blocker[F]
      tx   <- transactor[F](conf.db)

      migrator <- Resource.eval(migrator[F](conf.db))
      _        <- Resource.eval(migrator.migrate())

      userRepository         = UserRepository.of(tx)
      productRepository      = ProductRepository.of(tx)
      subscriptionRepository = SubscriptionRepository.of(tx)
      groupRepository        = ProductGroupRepository.of(tx)

      logger = LogManager.getLogger("notificator_layer")
      logHandler = LogHandler.of(
        (s: String) => logger.info(s).pure[F],
        (s: String) => logger.debug(s).pure[F],
        (s: String) => logger.error(s).pure[F]
      )

      configFile = getClass
        .getResource("/consumer.conf")
        .getPath

      kafkaConfig <- Resource.eval(
        Sync[F].delay(
          ConfigSource
            .file(configFile)
            .config()
        )
      )

      consumerConfig = kafkaConfig
        .map(config =>
          config
            .getConfig("notificator_consumer")
            .root()
            .unwrapped()
            .asScala
            .toMap
        )
        .getOrElse(Map.empty[String, AnyRef])

      productKafkaConsumerService = KafkaConsumerService
        .of[F, String, UUID](consumerConfig)

      _ <- Resource.eval(productKafkaConsumerService.subscribe(conf.email.productTopicName :: Nil))

      notificator = EmailNotificationService.of(
        conf.email,
        productRepository,
        userRepository,
        subscriptionRepository,
        groupRepository,
        be,
        logHandler,
        productKafkaConsumerService
      )
    } yield notificator
  }
}
