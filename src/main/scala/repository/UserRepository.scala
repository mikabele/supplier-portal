package repository

import cats.data.NonEmptyList
import cats.effect.Sync
import domain.user.ReadAuthorizedUser
import doobie.util.transactor.Transactor
import repository.impl.DoobieUserRepositoryImpl
import types.UuidStr

trait UserRepository[F[_]] {
  def getByIds(userIds: NonEmptyList[UuidStr]): F[List[ReadAuthorizedUser]]

  def getUsers(): F[List[ReadAuthorizedUser]]
}

object UserRepository {
  def of[F[_]: Sync](tx: Transactor[F]): UserRepository[F] = {
    new DoobieUserRepositoryImpl[F](tx)
  }
}
