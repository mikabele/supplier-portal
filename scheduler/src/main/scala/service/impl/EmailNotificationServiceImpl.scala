package service.impl

import cats.data.NonEmptyList
import cats.effect.{Async, ContextShift, Timer}
import cats.syntax.all._
import cats.{Monad, MonadError}
import com.emarsys.scheduler.Schedule
import com.emarsys.scheduler.syntax.toScheduleOps
import conf.app.EmailNotificatorConf
import domain.category.CategoryDomain
import domain.group.GroupReadDomain
import domain.product.ProductReadDomain
import domain.supplier.SupplierDomain
import domain.user.ClientDomain
import emil.builder._
import emil.{Emil, Mail, MailConfig, SSLType}
import kafka.KafkaConsumerService
import logger.LogHandler
import repository.{ProductGroupRepository, ProductRepository, SubscriptionRepository, UserRepository}
import service.EmailNotificationService

import java.time.LocalDateTime
import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.io.Source

case class EmailNotificationServiceImpl[F[+_]: Async: Monad: MonadError[*[_], Throwable]: Timer: ContextShift](
  emil:                   Emil[F],
  notificatorConf:        EmailNotificatorConf,
  productRepository:      ProductRepository[F],
  userRepository:         UserRepository[F],
  subscriptionRepository: SubscriptionRepository[F],
  groupRepository:        ProductGroupRepository[F],
  logger:                 LogHandler[F],
  productKafkaConsumer:   KafkaConsumerService[F, String, UUID]
) extends EmailNotificationService[F] {

  private val smtpConf = MailConfig(notificatorConf.url, notificatorConf.user, notificatorConf.password, SSLType.SSL)
  private val emilRun  = emil(smtpConf)

  private val newProductBody =
    Source
      .fromResource("text_patterns/new_product.html")
      .getLines()
      .reduce(_ |+| _)

  //TODO - try to use scalate
  private def beautifulProduct(product: ProductReadDomain): String = {
    val pictures = product.attachments
      .map(at =>
        s"""<li><img src="${at.attachment.value}" width="250" height="250" alt="${at.attachment.value}"></li>"""
      )
      .reduce(_ |+| _)
    newProductBody
      .replace("${product.name}", product.name.value)
      .replace("${product.category.name}", product.category.name.value)
      .replace("${product.price}", product.price.value.toString)
      .replace("${product.description}", product.description)
      .replace("${product.status}", product.status.toString)
      .replace("${product.publicationDate}", product.publicationDate.value)
      .replace("${product.supplier.name}", product.supplier.name.value)
      .replace("${product.supplier.address}", product.supplier.address.value)
      .replace("${product.attachments}", pictures)
      .replace("$$", "$")
  }

  private def createBodyByCategory(category: CategoryDomain, products: List[ProductReadDomain]): String = {
    s"""<div><hr><hr><h2>New products in category \'${category.name}\':</h2>""" +
      products.map(beautifulProduct).reduce(_ |+| _) + "</div>"
  }

  private def createBodyBySupplier(supplier: SupplierDomain, products: List[ProductReadDomain]): String = {
    s"""<div><hr><hr><h2>New products from supplier \'${supplier.name}\':</h2>""" +
      products
        .map(beautifulProduct)
        .reduce(_ |+| _) + "</div>"
  }

  private def createMailWithBody(client: ClientDomain, body: String): Mail[F] = {
    MailBuilder
      .fromSeq[F](Seq(HtmlBody(body)))
      .add(
        From(notificatorConf.email),
        To(client.email.value),
        Subject("New products in service SupplierPortal")
      )
      .build
  }

  private def createMail(
    client:      ClientDomain,
    newProducts: NonEmptyList[ProductReadDomain],
    groups:      List[GroupReadDomain]
  ): Mail[F] = {
    val groupsWithClient = groups.filter(_.userIds.contains(client.id))
    val productsInGroup = newProducts.filter(product =>
      !groups.flatMap(_.productIds).contains(product.id) || groupsWithClient.flatMap(_.productIds).contains(product.id)
    )
    val productsByCategories =
      client.categorySubs
        .map(category => (category, productsInGroup.filter(_.category == category)))
        .filter(_._2.nonEmpty)

    val productsBySuppliers = client.supplierSubs
      .map(supplier => (supplier, productsInGroup.filter(_.supplier == supplier)))
      .filter(_._2.nonEmpty)

    val body = {
      (if (productsByCategories.nonEmpty)
         productsByCategories.map { case (cat, prs) => createBodyByCategory(cat, prs) }.reduce(_ |+| _) //+ "\n"
       else "") |+|
        (if (productsBySuppliers.nonEmpty)
           productsBySuppliers.map { case (sup, prs) => createBodyBySupplier(sup, prs) }.reduce(_ |+| _) //+ "\n"
         else "")
    }

    createMailWithBody(client, body)
  }

  private def getMails(newProducts: NonEmptyList[ProductReadDomain]): F[List[Mail[F]]] = {
    for {
      clients <- userRepository.getAllClientsWithSubscriptions()
      _       <- logger.debug(s"All clients from DB: $clients")
      groups  <- groupRepository.showGroups()
      _       <- logger.debug(s"All groups in DB : $groups")
      mails    = clients.map(cl => createMail(cl, newProducts, groups))
    } yield mails
  }

  private def getProductsFromIds(newProductIds: List[UUID]): F[Option[NonEmptyList[ProductReadDomain]]] = {
    for {
      newProducts <- newProductIds.toNel.traverse(nel => productRepository.getByIds(nel))
    } yield newProducts.flatMap(_.toNel)
  }

  private def effect: F[Unit] = {
    for {
      newProductRecords <- productKafkaConsumer.poll(
        notificatorConf.productTopicName,
        notificatorConf.pollingTimeout.seconds
      )
      newProductIds = newProductRecords.map(_.value())
      newProducts  <- getProductsFromIds(newProductIds)
      _            <- logger.debug(s"New products in Kafka : $newProducts")
      mails        <- newProducts.map(nel => getMails(nel)).getOrElse(List.empty.pure[F])
      _            <- mails.traverse(mail => emilRun.send(mail))
      _            <- logger.info(s"Last notification date : ${LocalDateTime.now()}")
    } yield ()
  } // receiving data from db , sending email

  private val policy: Schedule[F, Any, Any] = Schedule.fixed(notificatorConf.delay.minute)

  def start(): F[Any] = effect runOn policy
}
