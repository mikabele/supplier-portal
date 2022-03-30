package controller

import cats.effect.kernel.Concurrent
import cats.implicits._
import dto.attachment._
import dto.criteria._
import dto.product._
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import service.ProductService

// TODO - why circeEntityDecoder def doesn't work and in general why implicits don't work UPD: maybe fixed but ...
// TODO - why Sync bound doesn't work but Concurrent does, why first is not a subtype of second

object ProductController {

  def routes[F[_]: Concurrent](productService: ProductService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
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
      for {
        result <- Ok(productService.deleteProduct(id))
      } yield result
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

    def searchByCriteria(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "product" / "search_by" =>
        val res = for {
          criteria <- req.as[CriteriaDto]
          result   <- productService.searchByCriteria(criteria)
        } yield result

        marshalResponse(res)
    }

    def marshalResponse[T, E[_]](
      result: F[Either[E, T]]
    )(
      implicit E: EntityEncoder[F, T]
    ): F[Response[F]] = {
      result
        .flatMap {
          case Left(error)     => BadRequest(error)
          case Right(response) => Ok(response)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage)
        }
    }

    addProduct() <+> updateProduct() <+> deleteProduct <+> viewProducts <+> attachToProduct <+> searchByCriteria
  }
}
