package service.impl

import cats.Monad
import cats.data.{Chain, EitherT, NonEmptyList}
import domain.user._
import dto.user.NonAuthorizedUserDto
import org.http4s.Request
import org.http4s.headers.Cookie
import org.reactormonk.{CryptoBits, PrivateKey}
import repository.UserRepository
import service.AuthenticationService
import service.error.auth.CookieError.CookieValidationFail
import service.error.general.GeneralError
import service.error.user.UserError.{InvalidUserOrPassword, InvalidUserRole}
import util.ConvertToErrorsUtil.{ErrorsOr, _}
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}
import util.ModelMapper.DtoToDomain._

import scala.io.Codec
import scala.util.Random

class AuthenticationServiceImpl[F[_]: Monad](userRepository: UserRepository[F]) extends AuthenticationService[F] {
  private val key    = PrivateKey(Codec.toUTF8(Random.alphanumeric.take(20).mkString("")))
  private val crypto = CryptoBits(key)
  private val clock  = java.time.Clock.systemUTC

  override def verifyLogin(nonAuthorizedUserDto: NonAuthorizedUserDto): F[ErrorsOr[String]] = {
    val res = for {
      userDomain <- validateUserDto(nonAuthorizedUserDto).toErrorsOr(fromValidatedNec)
      user       <- EitherT.fromOptionF(userRepository.tryGetUser(userDomain), Chain[GeneralError](InvalidUserOrPassword))
      token       = crypto.signToken(user.id.toString, clock.millis.toString)
    } yield token

    res.value
  }

  private def validateCookie(req: Request[F]): Either[String, String] = {
    for {
      header <- req.headers
        .get[Cookie]
        .toRight("Cookie parsing error")
      cookie <- header.values.toList
        .find(_.name == "authcookie")
        .toRight("Couldn't find the authcookie")
      _ = println(cookie.content)
      id <- crypto
        .validateSignedToken(cookie.content)
        .toRight("Cookie invalid")
    } yield id
  }

  override def retrieveUser(req: Request[F]): F[ErrorsOr[ReadAuthorizedUser]] = {
    val res = for {
      token <- EitherT.fromEither(validateCookie(req)).leftMap(ex => Chain[GeneralError](CookieValidationFail(ex)))
      _      = println(token)
      users <- userRepository.getByIds(NonEmptyList.of(token)).toErrorsOr
      user   = users.head
    } yield user

    res.value
  }

  override def checkRole(user: ReadAuthorizedUser, expectedRoles: List[Role]): ErrorsOr[Unit] = {
    Either.cond(expectedRoles.contains(user.role), (), Chain[GeneralError](InvalidUserRole(user.role, expectedRoles)))
  }
}
