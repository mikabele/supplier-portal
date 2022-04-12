package context

import cats.data.{Kleisli, OptionT}
import cats.effect.{Async, Resource}
import cats.implicits.toSemigroupKOps
import org.http4s.{AuthedRoutes, HttpApp, Request, Response}
import conf.app._
import conf.db._
import controller._
import domain.user.ReadAuthorizedUser
import org.http4s.dsl.Http4sDsl
import org.http4s.server.AuthMiddleware
import repository._
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

      authenticationService = AuthenticationService.of(userRepository)
      authenticationRoutes  = AuthenticationController.routes(authenticationService)

      productGroupService = ProductGroupService.of(productGroupRepository, userRepository, productRepository)
      productGroupRoutes  = ProductGroupController.routes(productGroupService)

      productService      = ProductService.of(productRepository, supplierRepository)
      productAuthedRoutes = ProductController.authedRoutes[F](productService)

      subscriptionRepository = SubscriptionRepository.of(tx)
      subscriptionService    = SubscriptionService.of[F](subscriptionRepository, supplierRepository)
      subscriptionRoutes     = SubscriptionController.routes[F](subscriptionService)

      orderRepository = OrderRepository.of(tx)
      orderService    = OrderService.of(orderRepository, productRepository)
      orderRoutes     = OrderController.routes(orderService)

      deliveryRepository = DeliveryRepository.of(tx)
      deliveryService    = DeliveryService.of(deliveryRepository, orderRepository)
      deliveryRoutes     = DeliveryController.routes(deliveryService)

      authUser = Kleisli(authenticationService.retrieveUser): Kleisli[
        F,
        Request[F],
        Either[String, ReadAuthorizedUser]
      ]
      onFailure  = Kleisli(req => OptionT.liftF(Forbidden(req.context))): AuthedRoutes[String, F]
      middleware = AuthMiddleware(authUser, onFailure)

      productRoutes = middleware(productAuthedRoutes)

    } yield (authenticationRoutes <+> productRoutes <+> subscriptionRoutes <+> orderRoutes <+> deliveryRoutes <+> productGroupRoutes).orNotFound
  }
}
