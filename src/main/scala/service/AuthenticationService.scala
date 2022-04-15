package service

import cats.effect.Sync
import domain.user.AuthorizedUserDomain
import dto.user.NonAuthorizedUserDto
import org.http4s.Request
import repository.UserRepository
import service.impl.AuthenticationServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

trait AuthenticationService[F[_]] {
  def verifyLogin(nonAuthorizedUserDto: NonAuthorizedUserDto): F[ErrorsOr[String]] // authorization method,send token

  def retrieveUser(req: Request[F]): F[Either[String, AuthorizedUserDomain]] // after authorization
}

object AuthenticationService {
  def of[F[_]: Sync](userRepository: UserRepository[F]): AuthenticationService[F] = {
    new AuthenticationServiceImpl[F](userRepository)
  }
}
