package controller

import cats.effect.kernel.Concurrent
import cats.implicits._
import dto.subscription.{CategorySubscriptionDto, SupplierSubscriptionDto}
import io.circe.generic.auto._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import service.SubscriptionService
import util.ResponseHandlingUtil.marshalResponse

object SubscriptionController {
  def routes[F[_]: Concurrent](subscriptionService: SubscriptionService[F]): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    def subscribeSupplier(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "subscription" / "supplier" =>
        val res = for {
          supplier <- req.as[SupplierSubscriptionDto]
          result   <- subscriptionService.subscribeSupplier(supplier)
        } yield result

        marshalResponse(res)
    }

    def subscribeCategory(): HttpRoutes[F] = HttpRoutes.of[F] {
      case req @ POST -> Root / "api" / "subscription" / "category" =>
        val res = for {
          category <- req.as[CategorySubscriptionDto]
          result   <- subscriptionService.subscribeCategory(category)
        } yield result

        marshalResponse(res)
    }

    subscribeSupplier() <+> subscribeCategory()
  }
}
