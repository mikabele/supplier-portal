package service

import cats.data.Chain
import cats.effect.kernel.Sync
import domain.user.{ReadAuthorizedUser, Role}
import dto.user.NonAuthorizedUserDto
import org.http4s.Request
import repository.UserRepository
import service.error.general.GeneralError
import service.error.user.UserError.InvalidUserRole
import service.impl.AuthenticationServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

trait AuthenticationService[F[_]] {
  def verifyLogin(nonAuthorizedUserDto: NonAuthorizedUserDto): F[ErrorsOr[String]] // authorization method,send token

  def retrieveUser(req: Request[F]): F[Either[String, ReadAuthorizedUser]] // after authorization
}

object AuthenticationService {
  def of[F[_]: Sync](userRepository: UserRepository[F]): AuthenticationService[F] = {
    new AuthenticationServiceImpl[F](userRepository)
  }
}
