package repository.impl

import cats.effect._
import domain.user.AuthorizedUser
import doobie._
import doobie.implicits._
import repository.UserRepository

class DoobieUserRepositoryImpl[F[_]: Async](tx: Transactor[F]) extends UserRepository[F] {

  private val findUserByLoginStr: Fragment = fr"SELECT id, login, password, role, phone, email FROM users"

  override def tryFindUserByLogin(login: String): F[Option[AuthorizedUser]] = {
    (findUserByLoginStr ++ fr"WHERE login = '$login'").query[AuthorizedUser].option.transact(tx)
  }
}
