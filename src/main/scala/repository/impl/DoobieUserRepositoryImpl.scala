package repository.impl

import cats.effect.Sync
import domain.user._
import doobie.implicits._
import doobie.util.transactor.Transactor
import repository.UserRepository
import repository.impl.logger.logger._
import doobie.refined.implicits._ //never delete this row

class DoobieUserRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends UserRepository[F] {

  private val selectUsersQuery = fr"SELECT id,name,surname,role,phone,email FROM public.user"

  override def getUsers(): F[List[ReadAuthorizedUser]] = {
    selectUsersQuery.query[ReadAuthorizedUser].to[List].transact(tx)
  }
}
