package service

import cats.data.ValidatedNel
import cats.effect.Sync
import domain.user.User
import dto.user.UserDto
import repository.UserRepository
import sun.security.util.Password
import service.error.user.UserAuthorizationError
import service.impl.UserAuthorizationServiceImpl

trait UserAuthorizationService[F[_]] {
  def authorizeUser(userDto: UserDto): F[Either[UserAuthorizationError, User]]
}

object UserAuthorizationService {
  def of[F[_]: Sync](userRepository: UserRepository[F]): UserAuthorizationService[F] =
    new UserAuthorizationServiceImpl[F](userRepository)
}
