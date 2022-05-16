package context

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.effect.{Async, Concurrent, ContextShift, Resource, Sync}
import cats.syntax.all._
import conf.app.AppConf
import conf.db.{migrator, transactor}
import controller._
import domain.user.AuthorizedUserDomain
import kafka.KafkaProducerService
import logger.LogHandler
import logger.impl.log4jLogHandler
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpApp, Request}
import pureconfig.ConfigSource
import repository._
import service._
import util.KafkaSerializationUtil.kafkaSerializer

import java.util.UUID
import scala.jdk.CollectionConverters._

object AppContext {

  private def getMiddleware[F[_]: Monad](
    authenticationService: AuthenticationService[F]
  )(
    implicit dsl: Http4sDsl[F]
  ): AuthMiddleware[F, AuthorizedUserDomain] = {
    import dsl._
    val authUser: Kleisli[F, Request[F], Either[String, AuthorizedUserDomain]] = Kleisli(
      authenticationService.retrieveUser
    )
    val onFailure  = Kleisli(req => OptionT.liftF(Forbidden(req.context))): AuthedRoutes[String, F]
    val middleware = AuthMiddleware(authUser, onFailure)
    middleware
  }

  private def initKafkaProducerService[F[_]: Async](
    conf: AppConf
  ): Resource[F, KafkaProducerService[F, String, UUID]] = {
    for {
      kafkaConfig <- Resource.eval(Sync[F].delay(ConfigSource.resources(conf.server.producerConfigPath).config()))

      producerConfig = kafkaConfig
        .map(config => config.root().unwrapped().asScala.toMap)
        .getOrElse(Map.empty[String, AnyRef])

      productKafkaProducerService <- Resource.eval(
        KafkaProducerService.of[F, String, UUID](producerConfig, conf.server.productTopicName)
      )
    } yield productKafkaProducerService
  }

  def setUp[F[_]: Async: ContextShift: Concurrent](conf: AppConf): Resource[F, HttpApp[F]] = {
    implicit val dsl:        Http4sDsl[F]  = new Http4sDsl[F] {}
    implicit val logHandler: LogHandler[F] = log4jLogHandler("root")
    for {
      tx <- transactor[F](conf.db)

      migrator <- Resource.eval(migrator[F](conf.db))
      _        <- Resource.eval(migrator.migrate())

      supplierRepository     = SupplierRepository.of(tx)
      userRepository         = UserRepository.of(tx)
      productGroupRepository = ProductGroupRepository.of(tx)
      productRepository      = ProductRepository.of(tx)
      orderRepository        = OrderRepository.of(tx)
      subscriptionRepository = SubscriptionRepository.of(tx)
      deliveryRepository     = DeliveryRepository.of(tx)
      categoryRepository     = CategoryRepository.of(tx)

      productKafkaProducerService <- initKafkaProducerService(conf)

      authenticationService = AuthenticationService.of(userRepository)
      productGroupService   = ProductGroupService.of(productGroupRepository, userRepository, productRepository)
      productService = ProductService.of(
        productRepository,
        supplierRepository,
        orderRepository,
        categoryRepository,
        productKafkaProducerService
      )
      subscriptionService = SubscriptionService.of[F](subscriptionRepository, supplierRepository, categoryRepository)
      orderService        = OrderService.of(orderRepository, productRepository)
      deliveryService     = DeliveryService.of(deliveryRepository, orderRepository)

      productGroupAuthedRoutes = ProductGroupController.authedRoutes(productGroupService)
      productAuthedRoutes      = ProductController.authedRoutes[F](productService)
      subscriptionAuthedRoutes = SubscriptionController.authedRoutes[F](subscriptionService)
      orderAuthedRoutes        = OrderController.authedRoutes(orderService)
      deliveryAuthedRoutes     = DeliveryController.authedRoutes(deliveryService)

      authedRoutes =
        productAuthedRoutes <+> subscriptionAuthedRoutes <+> orderAuthedRoutes <+> deliveryAuthedRoutes <+> productGroupAuthedRoutes

      middleware = getMiddleware(authenticationService)

      authenticationRoutes = AuthenticationController.routes(authenticationService)
      routes               = middleware(authedRoutes)

    } yield (authenticationRoutes <+> routes).orNotFound
  }
}
