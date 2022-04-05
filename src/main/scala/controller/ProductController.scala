package controller

import cats.effect.kernel.Concurrent
import cats.implicits._
import dto.attachment._
import dto.criteria._
import dto.product._
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.ProductService
import util.ResponseHandlingUtil.marshalResponse
import controller.implicits._ //never delete this row

// TODO - add tests (functional tests). Start server -> Http4s client -> assert /should be.

object ProductController {

  def routes[F[_]: Concurrent](productService: ProductService[F]): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    def addProduct(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "api" / "product" =>
      val res = for {
        product <- req.as[CreateProductDto]
        result  <- productService.addProduct(product)
      } yield result

      marshalResponse(res)
    }

    def updateProduct(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / "api" / "product" =>
      val res = for {
        product <- req.as[UpdateProductDto]
        result  <- productService.updateProduct(product)
      } yield result

      marshalResponse(res)
    }

    def deleteProduct(): HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / "api" / "product" / UUIDVar(id) =>
      val res = for {
        result <- productService.deleteProduct(id)
      } yield result

      marshalResponse(res)
    }

    def viewProducts(): HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "api" / "product" =>
      for {
        products <- productService.readProducts()
        response <- Ok(products)
      } yield response
    }

    def attachToProduct(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "api" / "product" / "attach" =>
      val res = for {
        attachment <- req.as[CreateAttachmentDto]
        result     <- productService.attach(attachment)
      } yield result

      marshalResponse(res)
    }

    def search(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "api" / "product" / "search" =>
      val res = for {
        criteria <- req.as[CriteriaDto]
        result   <- productService.searchByCriteria(criteria)
      } yield result

      marshalResponse(res)
    }

    addProduct() <+> updateProduct() <+> deleteProduct <+> viewProducts <+> attachToProduct <+> search
  }
}
