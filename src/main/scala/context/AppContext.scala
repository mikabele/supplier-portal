package context

import cats.data.{Kleisli, OptionT}
import cats.effect.{Async, Concurrent, ContextShift, Resource}
import cats.syntax.all._
import conf.app.AppConf
import conf.db.{migrator, transactor}
import controller._
import domain.user.AuthorizedUserDomain
import logger.LogHandler
import org.apache.logging.log4j.LogManager
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpApp, Request}
import repository._
import service._

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

      productService      = ProductService.of(productRepository, supplierRepository, orderRepository, logHandler)
      productAuthedRoutes = ProductController.authedRoutes[F](productService)

      subscriptionService      = SubscriptionService.of[F](subscriptionRepository, supplierRepository, logHandler)
      subscriptionAuthedRoutes = SubscriptionController.authedRoutes[F](subscriptionService)

      orderService      = OrderService.of(orderRepository, productRepository, logHandler)
      orderAuthedRoutes = OrderController.authedRoutes(orderService)

      deliveryService      = DeliveryService.of(deliveryRepository, orderRepository, logHandler)
      deliveryAuthedRoutes = DeliveryController.authedRoutes(deliveryService)

      authUser = Kleisli(authenticationService.retrieveUser): Kleisli[
        F,
        Request[F],
        Either[String, AuthorizedUserDomain]
      ]
      onFailure  = Kleisli(req => OptionT.liftF(Forbidden(req.context))): AuthedRoutes[String, F]
      middleware = AuthMiddleware(authUser, onFailure)

      authedRoutes =
        productAuthedRoutes <+> subscriptionAuthedRoutes <+> orderAuthedRoutes <+> deliveryAuthedRoutes <+> productGroupAuthedRoutes
      routes = middleware(authedRoutes)

    } yield (authenticationRoutes <+> routes).orNotFound
  }
}
