package scheduler

import cats.data.NonEmptyList
import cats.effect._
import cats.syntax.all._
import cats.{Monad, MonadError}
import com.emarsys.scheduler.Schedule
import com.emarsys.scheduler.syntax._
import conf.app.EmailNotificatorConf
import domain.attachment.AttachmentReadDomain
import domain.product.ProductReadDomain
import domain.user.AuthorizedUserDomain
import emil._
import emil.builder._
import emil.javamail._
import logger.LogHandler
import repository.{ProductRepository, SubscriptionRepository, UserRepository}

import java.net.URL
import java.time.LocalDateTime
import scala.concurrent.duration.DurationInt

case class EmailNotificator[F[+_]: Async: Monad: MonadError[*[_], Throwable]: Timer: ContextShift](
  host:                   EmailNotificatorConf,
  productRepository:      ProductRepository[F],
  userRepository:         UserRepository[F],
  subscriptionRepository: SubscriptionRepository[F],
  be:                     Blocker,
  logger:                 LogHandler[F]
) {

  private val myemil   = JavaMailEmil[F](be)
  private val smtpConf = MailConfig(host.url, host.user, host.password, SSLType.SSL)

  private def createAttachUrl(attachment: AttachmentReadDomain): AttachInputStream[F] = {
    AttachUrl(new URL(attachment.attachment.value), be, filename = "Attachment.jpg".some)
  }

  private def beautifulProduct(product: ProductReadDomain): String = {
    s"""====================================================
       |- Name: ${product.name.value}
       |    -- Category: ${product.category.toString}, 
       |    -- Price: ${product.price} $$, 
       |    -- Description: ${product.description}
       |    -- Status: ${product.status.toString}
       |    -- Publication date: ${product.publicationDate.value}
       |    -- Supplier: 
       |        --- Name: ${product.supplier.name.value}
       |        --- Address: ${product.supplier.address.value}
       |====================================================""".stripMargin
  }

  private def createMail(user: AuthorizedUserDomain, products: List[ProductReadDomain]): Mail[F] = {
    val attachments = products.flatMap(_.attachments.map(createAttachUrl))
    MailBuilder
      .fromSeq[F](
        Seq(
          TextBody(
            "There are some new products regarding your subscriptions: \n" +
              products.map(beautifulProduct).mkString("\n")
          )
        )
          ++ attachments
      )
      .add(
        From(host.email),
        To(user.email.value),
        Subject("New products in service SupplierPortal")
      )
      .build
  }

  private def sendMail(user: AuthorizedUserDomain, products: List[ProductReadDomain]): F[NonEmptyList[String]] = {
    val mail = createMail(user, products)
    for {
      _   <- logger.info(s"Sended mail : $mail")
      res <- myemil(smtpConf).send(mail)
      _   <- logger.debug(s"Sending result : $res")
    } yield res
  }

  private def effect: F[Unit] = {
    for {
      users <- userRepository.getAllClients()
      _     <- logger.debug(s"All clients from DB: $users")
      emailWithProductsList <- users.traverse(user =>
        productRepository.getNewProductsBySubscription(user).map(l => (user, l))
      )
      _ <- logger
        .info(s"New products from last notification date for user regarding users subscriptions: ${emailWithProductsList
          .map(uwp => s"user : ${uwp._1}, products : ${uwp._2}")
          .mkString("\\n")}")
      nonEmptyMails = emailWithProductsList
        .filter(uwp => {
          val (_, products) = uwp
          products.nonEmpty
        })
      _ <- nonEmptyMails.traverse(uwp => {
        val (user, products) = uwp
        sendMail(user, products)
      })
      _ <- subscriptionRepository.updateLastNotificationDate()
      _ <- logger.info(s"Last notification date : ${LocalDateTime.now()}")
    } yield ()
  } // receiving data from db , sending email

  private val policy: Schedule[F, Any, Any] = Schedule.fixed(host.delay.minute)

  def start: F[Any] = effect runOn policy
}
