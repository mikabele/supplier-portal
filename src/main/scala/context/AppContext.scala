package context

import cats.effect.{Async, Resource}
import conf.app.AppConf
import conf.db.{migrator, transactor}
import controller.ProductController
import org.http4s.HttpApp
import repository.ProductRepository
import service.ProductService

// TODO - why migrations don't work

object AppContext {
  def setUp[F[_]: Async](conf: AppConf): Resource[F, HttpApp[F]] = for {
    tx <- transactor[F](conf.db)

    migrator <- Resource.eval(migrator[F](conf.db))
    _        <- Resource.eval(migrator.migrate())

    productRepository = ProductRepository.of(tx)
    productService    = ProductService.of(productRepository)

    httpApp = ProductController.routes[F](productService).orNotFound
  } yield httpApp
}
