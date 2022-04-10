package controller

import cats.effect.kernel.Concurrent
import cats.implicits._
import domain.order._
import dto.order._
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.OrderService
import util.ResponseHandlingUtil.marshalResponse

object OrderController {
  def routes[F[_]: Concurrent](orderService: OrderService[F]): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    def createOrder(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "order" / UUIDVar(id) //temp
          =>
        val res = for {
          createDto <- req.as[OrderCreateDto]
          id        <- orderService.createOrder(id, createDto)
        } yield id

        marshalResponse(res)
    }

    def cancelOrder(): HttpRoutes[F] = HttpRoutes.of[F] {
      case PUT -> Root / "api" / "order" / UUIDVar(userId) //temp
          / UUIDVar(id) =>
        val res = for {
          result <- orderService.cancelOrder(userId, id)
        } yield result

        marshalResponse(res)
    }

    def viewActiveOrders(): HttpRoutes[F] = HttpRoutes.of[F] {
      case GET -> Root / "api" / "order" / UUIDVar(userId) //temp
          =>
        for {
          orders   <- orderService.viewActiveOrders(userId)
          response <- Ok(orders)
        } yield response
    }

    createOrder() <+> cancelOrder() <+> viewActiveOrders()
  }
}
