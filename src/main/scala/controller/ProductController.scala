package controller

import cats.effect.Concurrent
import cats.syntax.all._
import domain.user.{ReadAuthorizedUser, Role}
import dto.attachment._
import dto.criteria.CriteriaDto
import dto.product._
import io.circe.generic.auto._
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.ProductService
import util.ResponseHandlingUtil.marshalResponse

import java.util.UUID

object ProductController {

  def authedRoutes[F[_]: Concurrent](
    productService: ProductService[F]
  )(
    implicit dsl: Http4sDsl[F]
  ): AuthedRoutes[ReadAuthorizedUser, F] = {
    import dsl._

    def addProduct(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case req @ POST -> Root / "api" / "product" as user if user.role == Role.Manager =>
        val res = for {
          product <- req.req.as[ProductCreateDto]
          result  <- productService.addProduct(product)
        } yield result

        marshalResponse(res)
    }

    def updateProduct(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case req @ PUT -> Root / "api" / "product" as user if user.role == Role.Manager =>
        val res = for {
          product <- req.req.as[ProductUpdateDto]
          result  <- productService.updateProduct(product)
        } yield result

        marshalResponse(res)
    }

    def deleteProduct(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case DELETE -> Root / "api" / "product" / UUIDVar(id) as user if user.role == Role.Manager =>
        val res = for {
          result <- productService.deleteProduct(id)
        } yield result

        marshalResponse(res)
    }

    def viewProducts(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case GET -> Root / "api" / "product" as user if List(Role.Manager, Role.Client).contains(user.role) =>
        for {
          products <- Ok(productService.readProducts(UUID.fromString(user.id.value)))
        } yield products
    }

    def attachToProduct(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case req @ POST -> Root / "api" / "product" / "attachment" as user if user.role == Role.Manager =>
        val res = for {
          attachment <- req.req.as[AttachmentCreateDto]
          result     <- productService.attach(attachment)
        } yield result

        marshalResponse(res)
    }

    def removeAttachment(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case DELETE -> Root / "api" / "product" / "attachment" / UUIDVar(id) as user if user.role == Role.Manager =>
        val res = for {
          result <- productService.removeAttachment(id)
        } yield result

        marshalResponse(res)
    }

    def search(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case req @ POST -> Root / "api" / "product" / "search" as user if user.role == Role.Client =>
        val res = for {
          criteria <- req.req.as[CriteriaDto]
          result   <- productService.searchByCriteria(UUID.fromString(user.id.value), criteria)
        } yield result

        marshalResponse(res)
    }

    addProduct() <+> updateProduct() <+> deleteProduct <+> viewProducts <+> attachToProduct <+> search <+> removeAttachment()
  }
}
