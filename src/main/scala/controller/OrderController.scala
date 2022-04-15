package controller

import cats.effect.Sync
import cats.implicits._
import domain.order._
import domain.user.{AuthorizedUserDomain, Role}
import dto.order._
import io.circe.generic.auto._
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.OrderService
import service.error.user.UserError.InvalidUserRole
import util.ResponseHandlingUtil.marshalResponse

object OrderController {
  def authedRoutes[F[_]: Sync](
    orderService: OrderService[F]
  )(
    implicit dsl: Http4sDsl[F]
  ): AuthedRoutes[AuthorizedUserDomain, F] = {
    import dsl._

    def createOrder(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case req @ POST -> Root / "api" / "order" as user if user.role == Role.Client =>
        val res = for {
          createDto <- req.req.as[OrderCreateDto]
          id        <- orderService.createOrder(user, createDto)
        } yield id

        marshalResponse(res)

      case POST -> Root / "api" / "order" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Client)).message)
    }

    def cancelOrder(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case PUT -> Root / "api" / "order" / UUIDVar(id) as user if user.role == Role.Client =>
        val res = for {
          result <- orderService.cancelOrder(user, id)
        } yield result

        marshalResponse(res)

      case PUT -> Root / "api" / "order" / UUIDVar(_) as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Client)).message)
    }

    def viewActiveOrders(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case GET -> Root / "api" / "order" as user if List(Role.Client, Role.Courier).contains(user.role) =>
        for {
          orders   <- orderService.viewActiveOrders(user)
          response <- Ok(orders)
        } yield response

      case GET -> Root / "api" / "order" as user =>
        Forbidden(InvalidUserRole(user.role, List(Role.Client, Role.Courier)).message)
    }

    createOrder() <+> cancelOrder() <+> viewActiveOrders()
  }
}
