package context

import cats.effect.{Async, Resource}
import cats.implicits.toSemigroupKOps
import org.http4s.HttpApp
import conf.app._
import conf.db._
import controller._
import repository._
import service._

object AppContext {
  def setUp[F[_]: Async](conf: AppConf): Resource[F, HttpApp[F]] = {
    for {
      tx <- transactor[F](conf.db)

      migrator <- Resource.eval(migrator[F](conf.db))
      _        <- Resource.eval(migrator.migrate())

      supplierRepository     = SupplierRepository.of(tx)
      userRepository         = UserRepository.of(tx)
      productGroupRepository = ProductGroupRepository.of(tx)
      productRepository      = ProductRepository.of(tx)

      productGroupService = ProductGroupService.of(productGroupRepository, userRepository, productRepository)
      productGroupRoutes  = ProductGroupController.routes(productGroupService)

      productService = ProductService.of(productRepository, supplierRepository)
      productRoutes  = ProductController.routes[F](productService)

      subscriptionRepository = SubscriptionRepository.of(tx)
      subscriptionService    = SubscriptionService.of[F](subscriptionRepository, supplierRepository)
      subscriptionRoutes     = SubscriptionController.routes[F](subscriptionService)

      orderRepository = OrderRepository.of(tx)
      orderService    = OrderService.of(orderRepository, productRepository)
      orderRoutes     = OrderController.routes(orderService)

      deliveryRepository = DeliveryRepository.of(tx)
      deliveryService    = DeliveryService.of(deliveryRepository, orderRepository)
      deliveryRoutes     = DeliveryController.routes(deliveryService)
    } yield (productRoutes <+> subscriptionRoutes <+> orderRoutes <+> deliveryRoutes <+> productGroupRoutes).orNotFound
  }
}
