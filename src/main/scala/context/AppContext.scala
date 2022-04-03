package context

import cats.effect.{Async, Resource}
import cats.implicits.toSemigroupKOps
import org.http4s.HttpApp

import conf.app._
import conf.db._
import controller.{ProductController, SubscriptionController}
import repository.{ProductRepository, SubscriptionRepository}
import service.{ProductService, SubscriptionService}

object AppContext {
  def setUp[F[_]: Async](conf: AppConf): Resource[F, HttpApp[F]] = {
    for {
      tx <- transactor[F](conf.db)

      migrator <- Resource.eval(migrator[F](conf.db))
      _        <- Resource.eval(migrator.migrate())

      productRepository = ProductRepository.of(tx)
      productService    = ProductService.of(productRepository)
      productRoutes     = ProductController.routes[F](productService)

      subscriptionRepository = SubscriptionRepository.of(tx)
      subscriptionService    = SubscriptionService.of[F](subscriptionRepository)
      subscriptionRoutes     = SubscriptionController.routes[F](subscriptionService)

    } yield (productRoutes <+> subscriptionRoutes).orNotFound
  }
}
