package context

import cats.data.{Kleisli, OptionT}
import cats.effect.{Async, Concurrent, ContextShift, Resource, Sync}
import cats.syntax.all._
import conf.app.AppConf
import conf.db.{migrator, transactor}
import controller._
import domain.user.AuthorizedUserDomain
import kafka.KafkaProducerService
import logger.LogHandler
import org.apache.logging.log4j.LogManager
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

  def setUp[F[_]: Async: ContextShift: Concurrent](conf: AppConf): Resource[F, HttpApp[F]] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._
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

      configFile = this.getClass
        .getResource("/producer.conf")
        .getPath

      kafkaConfig <- Resource.eval(
        Sync[F].delay(
          ConfigSource
            .file(configFile)
            .config()
        )
      )

      producerConfig = kafkaConfig
        .map(config =>
          config
            .getConfig("producer")
            .root()
            .unwrapped()
            .asScala
            .toMap
        )
        .getOrElse(Map.empty[String, AnyRef])

      productKafkaProducerService = KafkaProducerService
        .of[F, String, UUID](producerConfig, conf.server.productTopicName)

      logger = LogManager.getLogger("root")
      logHandler = LogHandler.of(
        (s: String) => logger.info(s).pure[F],
        (s: String) => logger.debug(s).pure[F],
        (s: String) => logger.error(s).pure[F]
      )

      authenticationService = AuthenticationService.of(userRepository, logHandler)
      authenticationRoutes  = AuthenticationController.routes(authenticationService)

      productGroupService = ProductGroupService.of(
        productGroupRepository,
        userRepository,
        productRepository,
        logHandler
      )
      productGroupAuthedRoutes = ProductGroupController.authedRoutes(productGroupService)

      productService = ProductService.of(
        productRepository,
        supplierRepository,
        orderRepository,
        categoryRepository,
        logHandler,
        productKafkaProducerService
      )
      productAuthedRoutes = ProductController.authedRoutes[F](productService)

      subscriptionService = SubscriptionService
        .of[F](subscriptionRepository, supplierRepository, categoryRepository, logHandler)
      subscriptionAuthedRoutes = SubscriptionController.authedRoutes[F](subscriptionService)

      orderService      = OrderService.of(orderRepository, productRepository, logHandler)
      orderAuthedRoutes = OrderController.authedRoutes(orderService)

      deliveryService      = DeliveryService.of(deliveryRepository, orderRepository, logHandler)
      deliveryAuthedRoutes = DeliveryController.authedRoutes(deliveryService)

      authUser: Kleisli[
        F,
        Request[F],
        Either[String, AuthorizedUserDomain]
      ]          = Kleisli(authenticationService.retrieveUser)
      onFailure  = Kleisli(req => OptionT.liftF(Forbidden(req.context))): AuthedRoutes[String, F]
      middleware = AuthMiddleware(authUser, onFailure)

      authedRoutes =
        productAuthedRoutes <+> subscriptionAuthedRoutes <+> orderAuthedRoutes <+> deliveryAuthedRoutes <+> productGroupAuthedRoutes
      routes = middleware(authedRoutes)

    } yield (authenticationRoutes <+> routes).orNotFound
  }
}
