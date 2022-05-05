package repository

import cats.data.NonEmptyList
import cats.effect.Async
import domain.user.{AuthorizedUserDomain, ClientDomain, NonAuthorizedUserDomain}
import doobie.util.transactor.Transactor
import repository.impl.DoobieUserRepositoryImpl

trait UserRepository[F[_]] {
  def getAllClientsWithSubscriptions(): F[List[ClientDomain]]

  def getAllClients(): F[List[AuthorizedUserDomain]] //technical method

  def tryGetUser(userDomain: NonAuthorizedUserDomain): F[Option[AuthorizedUserDomain]]

  def getByIds(userIds: NonEmptyList[String]): F[List[AuthorizedUserDomain]]

  def getUsers(): F[List[AuthorizedUserDomain]] // technical method
}

object UserRepository {
  def of[F[_]: Async](tx: Transactor[F]): UserRepository[F] = {
    new DoobieUserRepositoryImpl[F](tx)
  }
}
