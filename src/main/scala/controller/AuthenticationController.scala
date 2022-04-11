package controller

import cats.effect.Concurrent
import cats.syntax.all._
import dto.user.NonAuthorizedUserDto
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.dsl.Http4sDsl
import org.http4s.{HttpRoutes, InvalidMessageBodyFailure, ResponseCookie}
import service.AuthenticationService
import util.ResponseHandlingUtil.errorsToHttpResponse

// TODO - add unique login in sql

object AuthenticationController {

  def routes[F[_]: Concurrent](authenticationService: AuthenticationService[F]): HttpRoutes[F] = {
    implicit val dsl: Http4sDsl[F] = new Http4sDsl[F] {}
    import dsl._

    def logIn(): HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "api" / "auth" =>
      val res = for {
        userDto <- req.as[NonAuthorizedUserDto]
        token   <- authenticationService.verifyLogin(userDto)
      } yield token

      res
        .flatMap {
          case Right(token) => Ok("Logged in!").map(_.addCookie(ResponseCookie("authcookie", token)))
          case Left(error)  => errorsToHttpResponse(error)
        }
        .handleErrorWith {
          case e: InvalidMessageBodyFailure => BadRequest(e.getMessage)
          case e => InternalServerError(e.getMessage)
        }
    }

    logIn()
  }
}
