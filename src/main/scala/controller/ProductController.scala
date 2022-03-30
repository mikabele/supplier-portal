package controller

import cats.{Bifunctor, Functor, Monad}
import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyChain, ValidatedNec}
import cats.implicits._
import cats.effect.Sync
import dto.attachment.CreateAttachmentDto
import dto.product.{CreateProductDto, UpdateProductDto}
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes, Response}
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.ProductService
import io.circe.generic.semiauto._
import io.circe.generic.auto._
import org.http4s.circe._
import io.circe.generic.JsonCodec
import io.circe.syntax._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.circe.CirceInstances._
import org.http4s.circe.CirceEntityCodec._
import org.http4s.circe.CirceEntityEncoder._
import org.http4s.implicits._
import service.error.product.ProductError
import dto.criteria._

object ProductController {

  def routes[F[_]: Sync](productService: ProductService[F]): HttpRoutes[F] = {
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

    // TODO - add automatic convertion for ID

    def deleteProduct(): HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / "api" / "product" / id =>
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

    def marshalResponse[T, E[_, _]](
      result: F[E[ProductError, T]]
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
