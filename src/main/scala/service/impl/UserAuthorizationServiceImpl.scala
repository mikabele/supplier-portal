package service.impl

import cats.Functor
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxEitherId, catsSyntaxEq}
import domain.user
import dto.user
import repository.UserRepository
import service.UserAuthorizationService
import service.error.user
import dto.user.UserDto
import service.error.user.UserAuthorizationError
import domain.user.User
import cats.instances._

class UserAuthorizationServiceImpl[F[_]](userRepository: UserRepository[F]) extends UserAuthorizationService[F] {
  override def authorizeUser(userDto: UserDto): F[Either[UserAuthorizationError, User]] = {
    for {
      password      <- userRepository.checkLoginAndGetPassword(userDto.login)
      _             <- checkPassword(userDto, password)
      validatedUser <- userDtoToUser(userDto)
    } yield validatedUser
  }

  private def checkPassword(userDto: UserDto, expectedPassword: String): Either[UserAuthorizationError, String] = {
    if (userDto.password === expectedPassword)
      userDto.password.asRight
    else
      UserAuthorizationError.InvalidPassword(userDto.password).asLeft
  }
}
