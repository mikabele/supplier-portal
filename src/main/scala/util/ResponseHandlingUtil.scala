package util

import cats.data.Chain
import cats.effect.kernel.Concurrent
import cats.implicits._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, Response}
import service.error.general.{BadRequestError, ErrorsOr, ForbiddenError, GeneralError, NotFoundError}
import service.error.validation.ValidationError
import controller.implicits._

object ResponseHandlingUtil {
  def errorsToHttpResponse[F[_]: Concurrent](
    errors: Chain[GeneralError]
  )(
    implicit dsl: Http4sDsl[F]
  ): F[Response[F]] = {
    import dsl._
    errors.toList.head match {
      case _: BadRequestError => BadRequest(errors.mkString_("\n"))
      case _: NotFoundError   => NotFound(errors.mkString_("\n"))
      case _: ForbiddenError  => Forbidden(errors.mkString_("\n"))
      case _: ValidationError => BadRequest(errors.mkString_("\n"))
    }
  }

  def marshalResponse[T, F[_]: Concurrent](
    result: F[ErrorsOr[T]]
  )(
    implicit E: EntityEncoder[F, T],
    dsl:        Http4sDsl[F]
  ): F[Response[F]] = {
    import dsl._
    result
      .flatMap {
        case Left(chain)     => errorsToHttpResponse(chain)
        case Right(response) => Ok(response)
      }
      .handleErrorWith { ex =>
        InternalServerError(ex.getMessage)
      }
  }
}
