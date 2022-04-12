package repository

import cats.data.NonEmptyList
import cats.effect.Sync
import domain.user.{NonAuthorizedUser, ReadAuthorizedUser}
import doobie.util.transactor.Transactor
import repository.impl.DoobieUserRepositoryImpl
import types.UuidStr

trait UserRepository[F[_]] {
  def tryGetUser(userDomain: NonAuthorizedUser): F[Option[ReadAuthorizedUser]]

  def getByIds(userIds: NonEmptyList[String]): F[List[ReadAuthorizedUser]]

  def getUsers(): F[List[ReadAuthorizedUser]]
}

object UserRepository {
  def of[F[_]: Sync](tx: Transactor[F]): UserRepository[F] = {
    new DoobieUserRepositoryImpl[F](tx)
  }
}
