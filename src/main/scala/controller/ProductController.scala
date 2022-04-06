package controller

import cats.{Applicative, Monad}
import cats.data.{Chain, EitherT}
import cats.effect.{Async, Concurrent, Sync}
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
import controller.implicits._
import service.error.general.{ErrorsOr, GeneralError}
import service.error.validation.ValidationError.InvalidJsonFormat

import scala.util.Try

// TODO - add tests (functional tests). Start server -> Http4s client -> assert /should be.

object ProductController {

  def routes[F[_]: Concurrent](productService: ProductService[F]): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    def addProduct(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "api" / "product" =>
      val res = for {
        product <- EitherT(
          req
            .as[CreateProductDto]
            .attempt
        ).leftMap(ex => Chain[GeneralError](InvalidJsonFormat(ex.getMessage)))
        result <- EitherT(productService.addProduct(product))
      } yield result

      marshalResponse(res.value)
    }

    def updateProduct(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / "api" / "product" =>
      val res = for {
        product <-
          EitherT(
            req
              .as[UpdateProductDto]
              .attempt
          ).leftMap(ex => Chain[GeneralError](InvalidJsonFormat(ex.getMessage)))

        result <- EitherT(productService.updateProduct(product))
      } yield result

      marshalResponse(res.value)
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

    def attachToProduct(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "product" / "attachment" =>
        val res = for {
          attachment <- EitherT(
            req
              .as[CreateAttachmentDto]
              .attempt
          ).leftMap(ex => Chain[GeneralError](InvalidJsonFormat(ex.getMessage)))
          result <- EitherT(productService.attach(attachment))
        } yield result

        marshalResponse(res.value)
    }

    def removeAttachment(): HttpRoutes[F] = HttpRoutes.of[F] {
      case DELETE -> Root / "api" / "product" / "attachment" / UUIDVar(id) =>
        val res = for {
          result <- productService.removeAttachment(id)
        } yield result

        marshalResponse(res)
    }

    def search(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "api" / "product" / "search" =>
      val res = for {
        criteria <- EitherT(
          req
            .as[CriteriaDto]
            .attempt
        ).leftMap(ex => Chain[GeneralError](InvalidJsonFormat(ex.getMessage)))
        result <- EitherT(productService.searchByCriteria(criteria))
      } yield result

      marshalResponse(res.value)
    }

    addProduct() <+> updateProduct() <+> deleteProduct <+> viewProducts <+> attachToProduct <+> search <+> removeAttachment()
  }
}
