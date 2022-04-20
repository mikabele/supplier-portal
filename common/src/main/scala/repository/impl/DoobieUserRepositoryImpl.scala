package repository.impl

import cats.data.NonEmptyList
import cats.effect.{Async, Sync}
import domain.user.{AuthorizedUserDomain, NonAuthorizedUserDomain}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.fragments._
import doobie.util.transactor.Transactor
import repository.UserRepository
import repository.impl.logger.logger._

import java.util.UUID

class DoobieUserRepositoryImpl[F[_]: Async](tx: Transactor[F]) extends UserRepository[F] {

  private val selectUsersQuery = fr"SELECT id,name,surname,role,phone,email FROM public.user"

  override def getUsers(): F[List[AuthorizedUserDomain]] = {
    selectUsersQuery.query[AuthorizedUserDomain].to[List].transact(tx)
  }

  override def getByIds(userIds: NonEmptyList[String]): F[List[AuthorizedUserDomain]] = {
    (selectUsersQuery ++ fr" WHERE " ++ in(fr"id", userIds.map(id => UUID.fromString(id))))
      .query[AuthorizedUserDomain]
      .to[List]
      .transact(tx)
  }

  override def tryGetUser(userDomain: NonAuthorizedUserDomain): F[Option[AuthorizedUserDomain]] = {
    (selectUsersQuery ++ fr" WHERE login = ${userDomain.login} AND password = ${userDomain.password}")
      .query[AuthorizedUserDomain]
      .option
      .transact(tx)
  }

  override def getAllClients(): F[List[AuthorizedUserDomain]] = {
    (selectUsersQuery ++ fr" WHERE role='client'::user_role").query[AuthorizedUserDomain].to[List].transact(tx)
  }
}
