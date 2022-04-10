package controller

import cats.effect.Concurrent
import cats.syntax.all._
import dto.group._
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.ProductGroupService
import util.ResponseHandlingUtil.marshalResponse

object ProductGroupController {
  def routes[F[_]: Concurrent](productGroupService: ProductGroupService[F]): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    def createGroup(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "api" / "product_group" =>
      val res = for {
        group  <- req.as[GroupCreateDto]
        result <- productGroupService.createGroup(group)
      } yield result

      marshalResponse(res)
    }

    def addProductsToGroup(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "product_group" / "products" =>
        val res = for {
          groupWithProducts <- req.as[GroupWithProductsDto]
          result            <- productGroupService.addProducts(groupWithProducts)
        } yield result

        marshalResponse(res)
    }

    def addUsersToGroup(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "product_group" / "users" =>
        val res = for {
          groupWithUsers <- req.as[GroupWithUsersDto]
          result         <- productGroupService.addUsers(groupWithUsers)
        } yield result

        marshalResponse(res)
    }

    def removeProductsFromGroup(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ DELETE -> Root / "api" / "product_group" / "products" =>
        val res = for {
          groupWithProducts <- req.as[GroupWithProductsDto]
          result            <- productGroupService.removeProducts(groupWithProducts)
        } yield result

        marshalResponse(res)
    }

    def removeUsersFromGroup(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ DELETE -> Root / "api" / "product_group" / "users" =>
        val res = for {
          groupWithUsers <- req.as[GroupWithUsersDto]
          result         <- productGroupService.removeUsers(groupWithUsers)
        } yield result

        marshalResponse(res)
    }

    def deleteGroup(): HttpRoutes[F] = HttpRoutes.of[F] { case DELETE -> Root / "api" / "product_group" / UUIDVar(id) =>
      val res = for {
        result <- productGroupService.deleteGroup(id)
      } yield result

      marshalResponse(res)
    }

    def viewGroups(): HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "api" / "product_group" =>
      for {
        groups   <- productGroupService.showGroups()
        response <- Ok(groups)
      } yield response
    }

    createGroup() <+> addUsersToGroup() <+> addProductsToGroup() <+> removeUsersFromGroup() <+>
      removeProductsFromGroup() <+> deleteGroup() <+> viewGroups()
  }
}
