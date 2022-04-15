package scheduler

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import cats.{Monad, MonadError}
import com.emarsys.scheduler.Schedule
import com.emarsys.scheduler.syntax._
import conf.app.EmailNotificatorConf
import domain.product.ProductReadDomain
import domain.user.AuthorizedUserDomain
import emil._
import emil.builder._
import emil.javamail._
import repository.{ProductRepository, SubscriptionRepository, UserRepository}

import scala.concurrent.duration.DurationInt

case class EmailNotificator[F[+_]: Async: Monad: MonadError[*[_], Throwable]: Timer: ContextShift](
  host:                   EmailNotificatorConf,
  be:                     Blocker,
  productRepository:      ProductRepository[F],
  userRepository:         UserRepository[F],
  subscriptionRepository: SubscriptionRepository[F]
) {

  private val myemil   = JavaMailEmil[F](be)
  private val smtpConf = MailConfig(host.url, host.user, host.password, SSLType.SSL)

  private def createMail(user: AuthorizedUserDomain, products: List[ProductReadDomain]): Mail[F] = {
    MailBuilder.build(
      From(host.email),
      To(user.email.value),
      TextBody(
        "There are some new products regarding your subscriptions: " +
          products.map(product => "- " + product.toString + "\n")
      )
    )
  }

  private def sendMail(user: AuthorizedUserDomain, products: List[ProductReadDomain]): F[NonEmptyList[String]] = {
    val mail = createMail(user, products)
    for {
      res <- myemil(smtpConf).send(mail)
    } yield res
  }

  private def effect: F[Unit] = {
    for {
      users <- userRepository.getAllClients()
      emailWithProductsList <- users.traverse(user =>
        productRepository.getNewProductsBySubscription(user).map(l => (user, l))
      )
      _ = println(emailWithProductsList)
      nonEmptyMails = emailWithProductsList
        .filter(uwp => {
          val (_, products) = uwp
          products.nonEmpty
        })
      _ = println(nonEmptyMails)
      _ <- nonEmptyMails.traverse(uwp => {
        val (user, products) = uwp
        sendMail(user, products)
      })
      _ <- subscriptionRepository.updateLastNotificationDate()
    } yield ()
  } // receiving data from db , sending email

  private val policy: Schedule[F, Any, Any] = Schedule.fixed(host.delay.minute)

  def start: F[Any] = effect runOn policy
}
