import cats.effect.{Async, Blocker, ContextShift, ExitCode, IO, IOApp, Resource, Sync, Timer}
import conf.app.{AppConf, EmailNotificatorConf}
import conf.db.{migrator, transactor}
import email.EmailContext
import io.circe.config.parser
import io.circe.generic.auto._
import kafka.KafkaConsumerService
import kafka.impl.KafkaConsumerServiceImpl
import logger.LogHandler
import logger.impl.log4jLogHandler
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

  private def initKafkaConsumerService[F[_]: Async](
    conf: EmailNotificatorConf
  ): Resource[F, KafkaConsumerServiceImpl[F, String, UUID]] = {
    for {
      kafkaConfig <- Resource.eval(Sync[F].delay(ConfigSource.resources(conf.consumerConfigPath).config()))

      consumerConfig = kafkaConfig
        .map(config => config.root().unwrapped().asScala.toMap)
        .getOrElse(Map.empty[String, AnyRef])

      productKafkaConsumerService <- Resource.eval(KafkaConsumerService.of[F, String, UUID](consumerConfig))
    } yield productKafkaConsumerService
  }

  def notificatorResource[F[+_]: Async: Timer: ContextShift](
  ): Resource[F, EmailNotificationService[F]] = {
    implicit val logHandler: LogHandler[F] = log4jLogHandler("notificator_layer")
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

      productKafkaConsumerService <- initKafkaConsumerService(conf.email)

      _ <- Resource.eval(productKafkaConsumerService.subscribe(conf.email.productTopicName :: Nil))

      emil <- Resource.eval(EmailContext.of(be))

      notificator = EmailNotificationService.of(
        emil,
        conf.email,
        productRepository,
        userRepository,
        subscriptionRepository,
        groupRepository,
        productKafkaConsumerService
      )
    } yield notificator
  }
}
