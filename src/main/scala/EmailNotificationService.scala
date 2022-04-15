import cats.effect._
import conf.app.AppConf
import conf.db.transactor
import io.circe.config.parser
import io.circe.generic.auto._
import repository._
import scheduler.EmailNotificator

object EmailNotificationService extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    notificatorResource[IO]().use(n => n.start).as(ExitCode.Success)
  }

  def notificatorResource[F[+_]: ConcurrentEffect: Timer: ContextShift](): Resource[F, EmailNotificator[F]] = {
    for {
      conf <- Resource.eval(parser.decodePathF[F, AppConf]("app"))
      be   <- Blocker[F]
      tx   <- transactor[F](conf.db)

      userRepository         = UserRepository.of(tx)
      productRepository      = ProductRepository.of(tx)
      subscriptionRepository = SubscriptionRepository.of(tx)

      notificator = EmailNotificator(conf.email, be, productRepository, userRepository, subscriptionRepository)
    } yield notificator
  }
}
