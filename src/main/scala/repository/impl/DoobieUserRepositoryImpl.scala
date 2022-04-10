package repository.impl

import cats.data.NonEmptyList
import cats.effect.Sync
import domain.user._
import doobie.implicits._
import doobie.util.transactor.Transactor
import repository.UserRepository
import repository.impl.logger.logger._
import doobie.refined.implicits._
import doobie.postgres.implicits._
import types.UuidStr
import doobie.util.fragments._

import java.util.UUID

class DoobieUserRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends UserRepository[F] {

  private val selectUsersQuery = fr"SELECT id,name,surname,role,phone,email FROM public.user"

  override def getUsers(): F[List[ReadAuthorizedUser]] = {
    selectUsersQuery.query[ReadAuthorizedUser].to[List].transact(tx)
  }

  override def getByIds(userIds: NonEmptyList[UuidStr]): F[List[ReadAuthorizedUser]] = {
    (selectUsersQuery ++ fr" WHERE " ++ in(fr"id", userIds.map(id => UUID.fromString(id.value))))
      .query[ReadAuthorizedUser]
      .to[List]
      .transact(tx)
  }
}
