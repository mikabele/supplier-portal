package controller

import cats.effect.Concurrent
import cats.syntax.all._
import dto.delivery.DeliveryCreateDto
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.DeliveryService
import util.ResponseHandlingUtil.marshalResponse

object DeliveryController {
  def routes[F[_]: Concurrent](deliveryService: DeliveryService[F]): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    def assignDelivery(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "delivery" / UUIDVar(courierId) //temp field
          =>
        val res = for {
          createDto <- req.as[DeliveryCreateDto]
          id        <- deliveryService.createDelivery(courierId, createDto)
        } yield id

        marshalResponse(res)
    }

    def delivered(): HttpRoutes[F] = HttpRoutes.of[F] {
      case PUT -> Root / "api" / "delivery" / UUIDVar(courierId) //temp field
          / UUIDVar(id) =>
        val res = for {
          result <- deliveryService.delivered(courierId, id)
        } yield result

        marshalResponse(res)
    }

    def showDeliveries(): HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "api" / "delivery" =>
      for {
        deliveries <- deliveryService.showDeliveries()
        result     <- Ok(deliveries)
      } yield result
    }

    assignDelivery() <+> delivered() <+> showDeliveries()
  }
}
