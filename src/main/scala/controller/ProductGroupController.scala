package controller

import cats.effect.Concurrent
import cats.syntax.all._
import domain.user.{ReadAuthorizedUser, Role}
import dto.group._
import io.circe.generic.auto._
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.ProductGroupService
import service.error.user.UserError.InvalidUserRole
import util.ResponseHandlingUtil.marshalResponse

object ProductGroupController {
  def authedRoutes[F[_]: Concurrent](
    productGroupService: ProductGroupService[F]
  )(
    implicit dsl: Http4sDsl[F]
  ): AuthedRoutes[ReadAuthorizedUser, F] = {
    import dsl._

    def createGroup(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case req @ POST -> Root / "api" / "product_group" as user if user.role == Role.Manager =>
        val res = for {
          group  <- req.req.as[GroupCreateDto]
          result <- productGroupService.createGroup(group)
        } yield result

        marshalResponse(res)

      case POST -> Root / "api" / "product_group" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def addProductsToGroup(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case req @ POST -> Root / "api" / "product_group" / "products" as user if user.role == Role.Manager =>
        val res = for {
          groupWithProducts <- req.req.as[GroupWithProductsDto]
          result            <- productGroupService.addProducts(groupWithProducts)
        } yield result

        marshalResponse(res)

      case POST -> Root / "api" / "product_group" / "products" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def addUsersToGroup(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case req @ POST -> Root / "api" / "product_group" / "users" as user if user.role == Role.Manager =>
        val res = for {
          groupWithUsers <- req.req.as[GroupWithUsersDto]
          result         <- productGroupService.addUsers(groupWithUsers)
        } yield result

        marshalResponse(res)

      case POST -> Root / "api" / "product_group" / "users" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def removeProductsFromGroup(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case req @ DELETE -> Root / "api" / "product_group" / "products" as user if user.role == Role.Manager =>
        val res = for {
          groupWithProducts <- req.req.as[GroupWithProductsDto]
          result            <- productGroupService.removeProducts(groupWithProducts)
        } yield result

        marshalResponse(res)

      case DELETE -> Root / "api" / "product_group" / "products" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def removeUsersFromGroup(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case req @ DELETE -> Root / "api" / "product_group" / "users" as user if user.role == Role.Manager =>
        val res = for {
          groupWithUsers <- req.req.as[GroupWithUsersDto]
          result         <- productGroupService.removeUsers(groupWithUsers)
        } yield result

        marshalResponse(res)

      case DELETE -> Root / "api" / "product_group" / "users" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def deleteGroup(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
      case DELETE -> Root / "api" / "product_group" / UUIDVar(id) as user if user.role == Role.Manager =>
        val res = for {
          result <- productGroupService.deleteGroup(id)
        } yield result

        marshalResponse(res)

      case DELETE -> Root / "api" / "product_group" / UUIDVar(_) as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Manager)).message)
    }

    def viewGroups(): AuthedRoutes[ReadAuthorizedUser, F] = AuthedRoutes.of[ReadAuthorizedUser, F] {
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
