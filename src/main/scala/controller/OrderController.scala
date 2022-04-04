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
import controller.implicits._ // never delete this row

object OrderController {
  def routes[F[_]: Concurrent](orderService: OrderService[F]): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    def createOrder(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "api" / "order" =>
      val res = for {
        createDto <- req.as[CreateOrderDto]
        id        <- orderService.createOrder(createDto)
      } yield id

      marshalResponse(res)
    }

    def updateOrder(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ PUT -> Root / "api" / "order" =>
      val test = UpdateOrderDto("a882068c-7920-4471-88cd-5c8c7f806ff9", OrderStatus.Ordered)
      val res = for {
        updateDto <- req.as[UpdateOrderDto]
        result    <- orderService.updateOrder(updateDto)
      } yield result

      marshalResponse(res)
    }

    def viewActiveOrders(): HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root / "api" / "order" =>
      for {
        orders   <- orderService.viewActiveOrders()
        response <- Ok(orders)
      } yield response
    }

    createOrder() <+> updateOrder() <+> viewActiveOrders()
  }
}
