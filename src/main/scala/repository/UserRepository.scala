package repository

import cats.effect.Async
import domain.user.AuthorizedUser
import doobie.util.transactor.Transactor
import repository.impl.DoobieUserRepositoryImpl
import service.error.user.UserAuthorizationError

// TODO - find out how to work with Doobie lib and implement method UserRepository.of

trait UserRepository[F[_]] {
  def tryFindUserByLogin(login: String): F[Option[AuthorizedUser]]
}

object UserRepository {
  def of[F[_]: Async](tx: Transactor[F]): UserRepository[F] = {
    new DoobieUserRepositoryImpl[F](tx)
  }
}
