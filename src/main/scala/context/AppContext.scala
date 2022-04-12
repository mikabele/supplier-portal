package context

import cats.data.{Kleisli, OptionT}
import cats.effect.{Async, Resource}
import cats.implicits.toSemigroupKOps
import org.http4s.{HttpApp, Request}
import conf.app._
import conf.db._
import controller._
import domain.user.ReadAuthorizedUser
import org.http4s.dsl.Http4sDsl
import repository._
import org.http4s.AuthedRoutes
import org.http4s.server.AuthMiddleware
import service._

object AppContext {
  def setUp[F[_]: Async](conf: AppConf): Resource[F, HttpApp[F]] = {
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

      authenticationService = AuthenticationService.of(userRepository)
      authenticationRoutes  = AuthenticationController.routes(authenticationService)

      productGroupService      = ProductGroupService.of(productGroupRepository, userRepository, productRepository)
      productGroupAuthedRoutes = ProductGroupController.authedRoutes(productGroupService)

      productService      = ProductService.of(productRepository, supplierRepository, orderRepository)
      productAuthedRoutes = ProductController.authedRoutes[F](productService)

      subscriptionRepository   = SubscriptionRepository.of(tx)
      subscriptionService      = SubscriptionService.of[F](subscriptionRepository, supplierRepository)
      subscriptionAuthedRoutes = SubscriptionController.authedRoutes[F](subscriptionService)

      orderService      = OrderService.of(orderRepository, productRepository)
      orderAuthedRoutes = OrderController.authedRoutes(orderService)

      deliveryRepository   = DeliveryRepository.of(tx)
      deliveryService      = DeliveryService.of(deliveryRepository, orderRepository)
      deliveryAuthedRoutes = DeliveryController.authedRoutes(deliveryService)

      authUser   = Kleisli(authenticationService.retrieveUser):           Kleisli[F, Request[F], Either[String, ReadAuthorizedUser]]
      onFailure  = Kleisli(req => OptionT.liftF(Forbidden(req.context))): AuthedRoutes[String, F]
      middleware = AuthMiddleware(authUser, onFailure)

      authedRoutes =
        productAuthedRoutes <+> subscriptionAuthedRoutes <+> orderAuthedRoutes <+> deliveryAuthedRoutes <+> productAuthedRoutes
      routes = middleware(authedRoutes)

    } yield (authenticationRoutes <+> routes).orNotFound
  }
}
