import cats.effect._
import cats.implicits.catsSyntaxApplicativeId
import conf.app.AppConf
import conf.db.transactor
import io.circe.config.parser
import io.circe.generic.auto._
import logger.LogHandler
import org.apache.logging.log4j.LogManager
import repository._
import scheduler.EmailNotificator

object EmailNotificationService extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    notificatorResource[IO]().use(n => n.start).as(ExitCode.Success)
  }

  def notificatorResource[F[+_]: Async: Timer: ContextShift](
  ): Resource[F, EmailNotificator[F]] = {
    for {
      conf <- Resource.eval(parser.decodePathF[F, AppConf]("app"))
      be   <- Blocker[F]
      tx   <- transactor[F](conf.db)

      userRepository         = UserRepository.of(tx)
      productRepository      = ProductRepository.of(tx)
      subscriptionRepository = SubscriptionRepository.of(tx)
      logger                 = LogManager.getLogger("notificator_layer")
      logHandler = LogHandler.of(
        (s: String) => logger.info(s).pure[F],
        (s: String) => logger.debug(s).pure[F],
        (s: String) => logger.error(s).pure[F]
      )
      notificator = EmailNotificator(
        conf.email,
        productRepository,
        userRepository,
        subscriptionRepository,
        be,
        logHandler
      )
    } yield notificator
  }
}
