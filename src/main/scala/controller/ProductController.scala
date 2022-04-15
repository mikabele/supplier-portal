package controller

import cats.effect.Sync
import cats.syntax.all._
import domain.user.{AuthorizedUserDomain, Role}
import dto.attachment._
import dto.criteria.CriteriaDto
import dto.product._
import io.circe.generic.auto._
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.ProductService
import service.error.user.UserError.InvalidUserRole
import util.ResponseHandlingUtil.marshalResponse

object ProductController {

  def authedRoutes[F[_]: Sync](
    productService: ProductService[F]
  )(
    implicit dsl: Http4sDsl[F]
  ): AuthedRoutes[AuthorizedUserDomain, F] = {
    import dsl._

    def addProduct(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case req @ POST -> Root / "api" / "product" as user if user.role == Role.Manager =>
        val res = for {
          product <- req.req.as[ProductCreateDto]
          result  <- productService.addProduct(product)
        } yield result

        marshalResponse(res)

      case POST -> Root / "api" / "product" as user => Forbidden(InvalidUserRole(user.role, List(Role.Manager)))
    }

    def updateProduct(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case req @ PUT -> Root / "api" / "product" as user if user.role == Role.Manager =>
        val res = for {
          product <- req.req.as[ProductUpdateDto]
          result  <- productService.updateProduct(product)
        } yield result

        marshalResponse(res)

      case PUT -> Root / "api" / "product" as user => Forbidden(InvalidUserRole(user.role, List(Role.Manager)))
    }

    def deleteProduct(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case DELETE -> Root / "api" / "product" / UUIDVar(id) as user if user.role == Role.Manager =>
        val res = for {
          result <- productService.deleteProduct(id)
        } yield result

        marshalResponse(res)
      case DELETE -> Root / "api" / "product" / UUIDVar(_) as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)))
    }

    def viewProducts(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case GET -> Root / "api" / "product" as user if List(Role.Manager, Role.Client).contains(user.role) =>
        for {
          products <- Ok(productService.readProducts(user))
        } yield products
      case GET -> Root / "api" / "product" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager, Role.Client)))
    }

    def attachToProduct(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case req @ POST -> Root / "api" / "product" / "attachment" as user if user.role == Role.Manager =>
        val res = for {
          attachment <- req.req.as[AttachmentCreateDto]
          result     <- productService.attach(attachment)
        } yield result

        marshalResponse(res)
      case POST -> Root / "api" / "product" / "attachment" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)))
    }

    def removeAttachment(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case DELETE -> Root / "api" / "product" / "attachment" / UUIDVar(id) as user if user.role == Role.Manager =>
        val res = for {
          result <- productService.removeAttachment(id)
        } yield result

        marshalResponse(res)
      case DELETE -> Root / "api" / "product" / "attachment" / UUIDVar(_) as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)))
    }

    def search(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case req @ POST -> Root / "api" / "product" / "search" as user if user.role == Role.Client =>
        val res = for {
          criteria <- req.req.as[CriteriaDto]
          result   <- productService.searchByCriteria(user, criteria)
        } yield result

        marshalResponse(res)
      case POST -> Root / "api" / "product" / "search" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Client)))
    }

    addProduct() <+> updateProduct() <+> deleteProduct() <+> viewProducts() <+> attachToProduct() <+> search() <+> removeAttachment()
  }
}
