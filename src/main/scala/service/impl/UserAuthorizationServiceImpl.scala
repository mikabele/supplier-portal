package service.impl

import cats.Monad
import cats.data.EitherT
import cats.implicits._
import domain.user._
import dto.user._
import repository.UserRepository
import service.UserAuthorizationService
import service.error.user.UserAuthorizationError
import service.error.user.UserAuthorizationError.{InvalidLogin, InvalidPassword, NotEnoughPermissions}

class UserAuthorizationServiceImpl[F[_]: Monad](userRepository: UserRepository[F]) extends UserAuthorizationService[F] {
  override def authorizeUser(
    userDto:      NonAuthorizedUserDto,
    expectedRole: Role
  ): F[Either[UserAuthorizationError, AuthorizedUser]] = {
    val res: EitherT[F, UserAuthorizationError, AuthorizedUser] = for {
      user <- EitherT.fromOptionF(userRepository.tryFindUserByLogin(userDto.login), InvalidLogin)
      _    <- EitherT(checkPassword(userDto, user.password.value).pure[F])
      _    <- EitherT(checkRole(user, expectedRole).pure[F])
    } yield user

    res.value
  }

  private def checkRole(user: AuthorizedUser, role: Role): Either[UserAuthorizationError, AuthorizedUser] = {
    Either.cond(role == user.role, user, NotEnoughPermissions(role))
  }

  private def checkPassword(
    userDto:          NonAuthorizedUserDto,
    expectedPassword: String
  ): Either[UserAuthorizationError, NonAuthorizedUserDto] = {
    Either.cond(expectedPassword == userDto.password, userDto, InvalidPassword(userDto.login))
  }
}
