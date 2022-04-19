package controller

import cats.effect.Sync
import cats.effect.Concurrent
import cats.syntax.all._
import domain.user.{AuthorizedUserDomain, Role}
import dto.delivery.DeliveryCreateDto
import error.user.UserError.InvalidUserRole
import io.circe.generic.auto._
import org.http4s.AuthedRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.DeliveryService
import util.ResponseHandlingUtil.marshalResponse

object DeliveryController {
  def authedRoutes[F[_]: Concurrent](
    deliveryService: DeliveryService[F]
  )(
    implicit dsl: Http4sDsl[F]
  ): AuthedRoutes[AuthorizedUserDomain, F] = {
    import dsl._

    def assignDelivery(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case req @ POST -> Root / "api" / "delivery" as courier if courier.role == Role.Courier =>
        val res = for {
          createDto <- req.req.as[DeliveryCreateDto]
          id        <- deliveryService.createDelivery(courier, createDto)
        } yield id

        marshalResponse(res)
      case POST -> Root / "api" / "delivery" as courier =>
        Forbidden(InvalidUserRole(courier.role, List(Role.Courier)).message)
    }

    def delivered(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case PUT -> Root / "api" / "delivery" / UUIDVar(id) as courier if courier.role == Role.Courier =>
        val res = for {
          result <- deliveryService.delivered(courier, id)
        } yield result

        marshalResponse(res)
      case PUT -> Root / "api" / "delivery" / UUIDVar(_) as courier =>
        Forbidden(InvalidUserRole(courier.role, List(Role.Courier)).message)
    }

    def showDeliveries(): AuthedRoutes[AuthorizedUserDomain, F] = AuthedRoutes.of[AuthorizedUserDomain, F] {
      case GET -> Root / "api" / "delivery" as courier if courier.role == Role.Courier =>
        for {
          deliveries <- deliveryService.showDeliveries()
          result     <- Ok(deliveries)
        } yield result

      case GET -> Root / "api" / "delivery" as courier =>
        Forbidden(InvalidUserRole(courier.role, List(Role.Courier)).message)
    }

    assignDelivery() <+> delivered() <+> showDeliveries()
  }
}
