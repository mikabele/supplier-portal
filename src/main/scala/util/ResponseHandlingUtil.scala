package util

import cats.data.Chain
import cats.effect.Sync
import cats.syntax.all._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityEncoder, InvalidMessageBodyFailure, Response}
import service.error.general._
import util.ConvertToErrorsUtil.ErrorsOr

object ResponseHandlingUtil {
  def errorsToHttpResponse[F[_]: Sync](
    errors: Chain[GeneralError]
  )(
    implicit dsl: Http4sDsl[F]
  ): F[Response[F]] = {
    import dsl._
    val errorsString = errors.map(_.message).toList.fold("")(_ |+| _ |+| "\n")
    errors.toList.head match {
      case _: BadRequestError => BadRequest(errorsString)
      case _: NotFoundError   => NotFound(errorsString)
      case _: ForbiddenError  => Forbidden(errorsString)
      //case _: UnauthorizedError => Unauthorized.apply(errorsString)
      case _ => BadRequest(errorsString)
    }
  }

  def marshalResponse[T, F[_]: Sync](
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
      .handleErrorWith {
        case e: InvalidMessageBodyFailure => BadRequest(e.getMessage)
        case e => InternalServerError(e.getMessage)
      }
  }
}
