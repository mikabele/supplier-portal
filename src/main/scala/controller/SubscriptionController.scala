package controller

import cats.data.Chain
import cats.effect.kernel.Concurrent
import cats.implicits._
import dto.subscription.{CategorySubscriptionDto, SupplierSubscriptionDto}
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, HttpRoutes, Response}
import service.SubscriptionService
import service.error.general.{BadRequestError, ErrorsOr, ForbiddenError, GeneralError, NotFoundError}
import service.error.validation.ValidationError

// TODO - why OK(int) doesn't work

object SubscriptionController {
  def routes[F[_]: Concurrent](subscriptionService: SubscriptionService[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
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

    def errorsToHttpResponse(errors: Chain[GeneralError]): F[Response[F]] = errors.toList.head match {
      case _: BadRequestError => BadRequest(errors.mkString_("\n"))
      case _: NotFoundError   => NotFound(errors.mkString_("\n"))
      case _: ForbiddenError  => Forbidden(errors.mkString_("\n"))
      case _: ValidationError => BadRequest(errors.mkString_("\n"))
    }

    def marshalResponse[T](
      result: F[ErrorsOr[T]]
    )(
      implicit E: EntityEncoder[F, T]
    ): F[Response[F]] = {
      result
        .flatMap {
          case Left(chain)     => errorsToHttpResponse(chain)
          case Right(response) => Ok(response.toString)
        }
        .handleErrorWith { ex =>
          InternalServerError(ex.getMessage)
        }
    }

    subscribeSupplier() <+> subscribeCategory()
  }
}
