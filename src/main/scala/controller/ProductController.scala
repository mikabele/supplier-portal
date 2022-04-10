package controller

import cats.effect.Concurrent
import cats.syntax.all._
import dto.attachment._
import dto.criteria._
import dto.product._
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.ProductService
import util.ResponseHandlingUtil.marshalResponse

object ProductController {

  def routes[F[_]: Concurrent](productService: ProductService[F]): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    def addProduct(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "api" / "product" =>
      val res = for {
        product <- req.as[ProductCreateDto]
        result  <- productService.addProduct(product)
      } yield result

      marshalResponse(res)
    }

    def updateProduct(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / "api" / "product" =>
      val res = for {
        product <- req.as[ProductUpdateDto]
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

    def viewProducts(): HttpRoutes[F] = HttpRoutes.of[F] {
      case GET -> Root / "api" / "product" / UUIDVar(userId) //temp
          =>
        for {
          products <- productService.readProducts(userId)
          response <- Ok(products)
        } yield response
    }

    def attachToProduct(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "product" / "attachment" =>
        val res = for {
          attachment <- req.as[AttachmentCreateDto]
          result     <- productService.attach(attachment)
        } yield result

        marshalResponse(res)
    }

    def removeAttachment(): HttpRoutes[F] = HttpRoutes.of[F] {
      case DELETE -> Root / "api" / "product" / "attachment" / UUIDVar(id) =>
        val res = for {
          result <- productService.removeAttachment(id)
        } yield result

        marshalResponse(res)
    }

    def search(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "product" / "search" / UUIDVar(userId) //temp
          =>
        val res = for {
          criteria <- req.as[CriteriaDto]
          result   <- productService.searchByCriteria(userId, criteria)
        } yield result

        marshalResponse(res)
    }

    addProduct() <+> updateProduct() <+> deleteProduct <+> viewProducts <+> attachToProduct <+> search <+> removeAttachment()
  }
}
