package repository.impl

import cats.data.NonEmptyList
import cats.effect.Async
import domain.subscription.{CategorySubscriptionReadDomain, SupplierSubscriptionReadDomain}
import domain.user.{AuthorizedUserDomain, ClientDbDomain, ClientDomain, NonAuthorizedUserDomain}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.fragments._
import doobie.util.transactor.Transactor
import repository.UserRepository
import repository.impl.logger.logger._
import util.ModelMapper.DbModelMapper.joinClientsWithSubscriptions

import java.util.UUID

// TODO - split getClients with subscription method into 3 methods separately (get clients, get cats,get sups,join it)

class DoobieUserRepositoryImpl[F[_]: Async](tx: Transactor[F]) extends UserRepository[F] {

  private val selectUsersQuery   = fr"SELECT id,name,surname,role,phone,email FROM public.user"
  private val selectClientsQuery = fr"SELECT id,email FROM public.user WHERE role = 'client'::user_role"

  private val getCategorySubQuery =
    fr"SELECT cs.user_id,c.id,c.name FROM category AS c INNER JOIN category_subscription AS cs ON c.id=cs.category_id"
  private val getSupplierSubQuery =
    fr"SELECT ss.user_id,s.id,s.name,s.address FROM supplier AS s INNER JOIN supplier_subscription AS ss ON s.id=ss.supplier_id "

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

  override def getAllClientsWithSubscriptions(): F[List[ClientDomain]] = {
    val res = for {
      clients <- selectClientsQuery.query[ClientDbDomain].to[List]
      cats    <- getCategorySubQuery.query[CategorySubscriptionReadDomain].to[List]
      sups    <- getSupplierSubQuery.query[SupplierSubscriptionReadDomain].to[List]
    } yield joinClientsWithSubscriptions(clients, cats, sups)

    res.transact(tx)
  }
}
