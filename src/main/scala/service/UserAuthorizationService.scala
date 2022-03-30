package service

import cats.effect.Sync
import domain.user._
import dto.user._
import repository.UserRepository
import service.error.user.UserAuthorizationError
import service.impl.UserAuthorizationServiceImpl

trait UserAuthorizationService[F[_]] {
  def authorizeUser(
    userDto:      NonAuthorizedUserDto,
    expectedRole: Role
  ): F[Either[UserAuthorizationError, AuthorizedUser]]
}

object UserAuthorizationService {
  def of[F[_]: Sync](userRepository: UserRepository[F]): UserAuthorizationService[F] =
    new UserAuthorizationServiceImpl[F](userRepository)
}
