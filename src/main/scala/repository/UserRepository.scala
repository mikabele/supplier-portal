package repository

import cats.data.NonEmptyList
import cats.effect.Sync
import domain.user.{AuthorizedUserDomain, NonAuthorizedUserDomain}
import doobie.util.transactor.Transactor
import repository.impl.DoobieUserRepositoryImpl

trait UserRepository[F[_]] {
  def getAllClients(): F[List[AuthorizedUserDomain]] //technical method

  def tryGetUser(userDomain: NonAuthorizedUserDomain): F[Option[AuthorizedUserDomain]]

  def getByIds(userIds: NonEmptyList[String]): F[List[AuthorizedUserDomain]]

  def getUsers(): F[List[AuthorizedUserDomain]] // technical method
}

object UserRepository {
  def of[F[_]: Sync](tx: Transactor[F]): UserRepository[F] = {
    new DoobieUserRepositoryImpl[F](tx)
  }
}
