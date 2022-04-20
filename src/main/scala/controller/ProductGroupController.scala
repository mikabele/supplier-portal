package controller

import cats.effect.Concurrent
import cats.syntax.all._
import domain.user.{AuthorizedUserDomain, Role}
import dto.group._
import error.user.UserError.InvalidUserRole
import io.circe.generic.auto._
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.ProductGroupService
import util.ResponseHandlingUtil.marshalResponse

object ProductGroupController {
  def authedRoutes[F[_]: Concurrent](
    productGroupService: ProductGroupService[F]
  )(
    implicit dsl: Http4sDsl[F]
  ): AuthedRoutes[AuthorizedUserDomain, F] = {
    import dsl._

    def createGroup(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case req @ POST -> Root / "api" / "product_group" as user if user.role == Role.Manager =>
        val res = for {
          group  <- req.req.as[GroupCreateDto]
          result <- productGroupService.createGroup(group)
        } yield result

        marshalResponse(res)

      case POST -> Root / "api" / "product_group" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def addProductsToGroup(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case req @ POST -> Root / "api" / "product_group" / "products" as user if user.role == Role.Manager =>
        val res = for {
          groupWithProducts <- req.req.as[GroupWithProductsDto]
          result            <- productGroupService.addProducts(groupWithProducts)
        } yield result

        marshalResponse(res)

      case POST -> Root / "api" / "product_group" / "products" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def addUsersToGroup(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case req @ POST -> Root / "api" / "product_group" / "users" as user if user.role == Role.Manager =>
        val res = for {
          groupWithUsers <- req.req.as[GroupWithUsersDto]
          result         <- productGroupService.addUsers(groupWithUsers)
        } yield result

        marshalResponse(res)

      case POST -> Root / "api" / "product_group" / "users" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def removeProductsFromGroup(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case req @ DELETE -> Root / "api" / "product_group" / "products" as user if user.role == Role.Manager =>
        val res = for {
          groupWithProducts <- req.req.as[GroupWithProductsDto]
          result            <- productGroupService.removeProducts(groupWithProducts)
        } yield result

        marshalResponse(res)

      case DELETE -> Root / "api" / "product_group" / "products" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def removeUsersFromGroup(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case req @ DELETE -> Root / "api" / "product_group" / "users" as user if user.role == Role.Manager =>
        val res = for {
          groupWithUsers <- req.req.as[GroupWithUsersDto]
          result         <- productGroupService.removeUsers(groupWithUsers)
        } yield result

        marshalResponse(res)

      case DELETE -> Root / "api" / "product_group" / "users" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def deleteGroup(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case DELETE -> Root / "api" / "product_group" / UUIDVar(id) as user if user.role == Role.Manager =>
        val res = for {
          result <- productGroupService.deleteGroup(id)
        } yield result

        marshalResponse(res)

      case DELETE -> Root / "api" / "product_group" / UUIDVar(_) as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def viewGroups(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case GET -> Root / "api" / "product_group" as user if user.role == Role.Manager =>
        for {
          groups   <- productGroupService.showGroups()
          response <- Ok(groups)
        } yield response

      case GET -> Root / "api" / "product_group" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    createGroup() <+> addUsersToGroup() <+> addProductsToGroup() <+> removeUsersFromGroup() <+>
      removeProductsFromGroup() <+> deleteGroup() <+> viewGroups()
  }
}
