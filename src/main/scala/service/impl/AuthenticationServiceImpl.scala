package service.impl

import cats.Monad
import cats.data.{Chain, EitherT, NonEmptyList}
import domain.user._
import dto.user.NonAuthorizedUserDto
import logger.LogHandler
import org.http4s.{headers, Request}
import org.reactormonk.{CryptoBits, PrivateKey}
import repository.UserRepository
import service.AuthenticationService
import service.error.general.GeneralError
import service.error.user.UserError.{InvalidUserOrPassword, UserNotFound}
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}
import util.ConvertToErrorsUtil.{ErrorsOr, _}
import util.ModelMapper.DtoToDomain._

import scala.io.Codec
import scala.util.Random

// TODO - add unit tests
// TODO - add multimodule architecture
// TODO - check data validation
// TODO - create demo for project (продемонстрировать работу приложения, запустить все компоненты, рассказать всю функциональность
//  при помощи функциональных тестов, рассказать про фичи, чему научился, что еще не сделал, можно сделать презентацию,
//  что можно добавить, попытаться продать свой продукт(типо))
// TODO - add docker
// TODO - add README
// TODO - (optional) kafka
// TODO - merge sql files

class AuthenticationServiceImpl[F[_]: Monad](
  userRepository: UserRepository[F],
  logHandler:     LogHandler[F]
) extends AuthenticationService[F] {
  private val key    = PrivateKey(Codec.toUTF8(Random.alphanumeric.take(20).mkString("")))
  private val crypto = CryptoBits(key)
  private val clock  = java.time.Clock.systemUTC

  override def verifyLogin(nonAuthorizedUserDto: NonAuthorizedUserDto): F[ErrorsOr[String]] = {
    val res = for {
      _          <- logHandler.debug("Start validation : NonAuthorizedUserDto").toErrorsOr
      userDomain <- validateUserDto(nonAuthorizedUserDto).toErrorsOr(fromValidatedNec)
      _          <- logHandler.debug("Validation finished: NonAuthorizedUserDto").toErrorsOr
      user       <- EitherT.fromOptionF(userRepository.tryGetUser(userDomain), Chain[GeneralError](InvalidUserOrPassword))
      _          <- logHandler.info(s"User found, id = ${user.id}").toErrorsOr
      token       = crypto.signToken(user.id.toString, clock.millis.toString)
    } yield token

    res.value
  }

  private def validateCookie(req: Request[F]): Either[String, String] = {
    for {
      header <- headers.Cookie.from(req.headers).toRight("Cookie parsing error")
      cookie <- header.values.toList
        .find(_.name == "authcookie")
        .toRight("Couldn't find the authcookie")
      id <- crypto
        .validateSignedToken(cookie.content)
        .toRight("Cookie invalid")
    } yield id
  }

  override def retrieveUser(req: Request[F]): F[Either[String, AuthorizedUserDomain]] = {
    val res = for {
      _     <- EitherT.liftF[F, String, Unit](logHandler.debug("Start validation: cookie"))
      token <- EitherT.fromEither(validateCookie(req))
      _     <- EitherT.liftF[F, String, Unit](logHandler.debug("Validation Finished: cookie"))
      users <- EitherT.liftF(userRepository.getByIds(NonEmptyList.of(token)))
      user  <- EitherT.fromOption(users.headOption, UserNotFound(token).message)
      _     <- EitherT.liftF[F, String, Unit](logHandler.info(s"User found, id ${user.id}"))
    } yield user

    res.value
  }
}
