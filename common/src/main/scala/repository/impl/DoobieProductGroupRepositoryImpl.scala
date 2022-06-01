package repository.impl

import cats.effect.{Async, Sync}
import cats.syntax.all._
import domain.group.{
  GroupCreateDomain,
  GroupProductDomain,
  GroupReadDbDomain,
  GroupReadDomain,
  GroupUserDomain,
  GroupWithProductsDomain,
  GroupWithUsersDomain
}
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.transactor.Transactor
import util.ModelMapper.DbModelMapper._
import doobie.util.fragments._
import repository.ProductGroupRepository
import repository.impl.logger.logger._

import java.util.UUID

class DoobieProductGroupRepositoryImpl[F[_]: Async](tx: Transactor[F]) extends ProductGroupRepository[F] {

  private val createProductGroupQuery  = fr"INSERT INTO public.group(name) VALUES "
  private val deleteProductGroupQuery  = fr"DELETE FROM public.group "
  private val selectProductGroupsQuery = fr"SELECT id,name FROM public.group "
  private val selectGroupUsersQuery    = fr"SELECT user_id,group_id FROM group_to_user "
  private val selectGroupProductsQuery = fr"SELECT product_id,group_id FROM group_to_product "
  private val addProductsQuery         = fr"INSERT INTO group_to_product(group_id,product_id) SELECT "
  private val addUsersQuery            = fr"INSERT INTO group_to_user(group_id,user_id) SELECT "
  private val deleteUsersQuery         = fr"DELETE FROM group_to_user "
  private val deleteProductsQuery      = fr"DELETE FROM group_to_product "
  private val getByNameQuery           = fr"SELECT 1 WHERE EXISTS ( SELECT * FROM public.group "

  override def addProducts(domain: GroupWithProductsDomain): F[Int] = {
    (addProductsQuery ++ fr"${domain.id}::UUID,unnest(${domain.productIds.map(_.value).toList}::UUID[])").update.run
      .transact(tx)
  }

  override def addUsers(domain: GroupWithUsersDomain): F[Int] = {
    (addUsersQuery ++ fr"${domain.id}::UUID,unnest(${domain.userIds.map(_.value).toList}::UUID[])").update.run
      .transact(tx)
  }

  override def removeProducts(domain: GroupWithProductsDomain): F[Int] = {
    (deleteProductsQuery ++ fr" WHERE group_id = ${domain.id}::UUID AND " ++ in(
      fr"product_id",
      domain.productIds.map(id => UUID.fromString(id.value))
    )).update.run.transact(tx)
  }

  override def removeUsers(domain: GroupWithUsersDomain): F[Int] = {
    (deleteUsersQuery ++ fr" WHERE group_id = ${domain.id}::UUID AND " ++ in(
      fr"user_id",
      domain.userIds.map(id => UUID.fromString(id.value))
    )).update.run
      .transact(tx)
  }

  override def addGroup(domain: GroupCreateDomain): F[UUID] = {
    (createProductGroupQuery ++ fr"(${domain.name})").update.withUniqueGeneratedKeys[UUID]("id").transact(tx)
  }

  override def deleteGroup(id: UUID): F[Int] = {
    val res = for {
      _     <- (deleteProductsQuery ++ fr" WHERE group_id = $id").update.run
      _     <- (deleteUsersQuery ++ fr" WHERE group_id = $id").update.run
      count <- (deleteProductGroupQuery ++ fr" WHERE id = $id").update.run
    } yield count

    res.transact(tx)
  }

  override def showGroups(): F[List[GroupReadDomain]] = {
    for {
      groups   <- selectProductGroupsQuery.query[GroupReadDbDomain].to[List].transact(tx)
      users    <- selectGroupUsersQuery.query[GroupUserDomain].to[List].transact(tx)
      products <- selectGroupProductsQuery.query[GroupProductDomain].to[List].transact(tx)
    } yield joinGroupsWithUsersAndProducts(groups, users, products)
  }

  override def getById(id: UUID): F[Option[GroupReadDomain]] = {
    for {
      group <- (selectProductGroupsQuery ++ fr" WHERE id = $id").query[GroupReadDbDomain].option.transact(tx)
      users <- (selectGroupUsersQuery ++ fr" WHERE group_id = $id").query[GroupUserDomain].to[List].transact(tx)
      products <- (selectGroupProductsQuery ++ fr" WHERE group_id = $id")
        .query[GroupProductDomain]
        .to[List]
        .transact(tx)
    } yield group.map(g => GroupReadDomain(g.id, g.name, users.map(_.userId), products.map(_.productId)))
  }

  override def checkByName(name: String): F[Option[Int]] = {
    (getByNameQuery ++ fr" WHERE name = $name)").query[Int].option.transact(tx)
  }

}
