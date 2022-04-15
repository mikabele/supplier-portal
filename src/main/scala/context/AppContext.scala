package context

import cats.data.{Kleisli, OptionT}
import cats.effect.{Async, ContextShift, Resource, Timer}
import cats.syntax.all._
import conf.app._
import conf.db._
import controller._
import domain.user.AuthorizedUserDomain
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, HttpApp, Request}
import repository._
import service._

object AppContext {
  def setUp[F[_]: ContextShift: Async: Timer](conf: AppConf): Resource[F, HttpApp[F]] = {
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

      authenticationService = AuthenticationService.of(userRepository)
      authenticationRoutes  = AuthenticationController.routes(authenticationService)

      productGroupService      = ProductGroupService.of(productGroupRepository, userRepository, productRepository)
      productGroupAuthedRoutes = ProductGroupController.authedRoutes(productGroupService)

      productService      = ProductService.of(productRepository, supplierRepository, orderRepository)
      productAuthedRoutes = ProductController.authedRoutes[F](productService)

      subscriptionService      = SubscriptionService.of[F](subscriptionRepository, supplierRepository)
      subscriptionAuthedRoutes = SubscriptionController.authedRoutes[F](subscriptionService)

      orderService      = OrderService.of(orderRepository, productRepository)
      orderAuthedRoutes = OrderController.authedRoutes(orderService)

      deliveryRepository   = DeliveryRepository.of(tx)
      deliveryService      = DeliveryService.of(deliveryRepository, orderRepository)
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
