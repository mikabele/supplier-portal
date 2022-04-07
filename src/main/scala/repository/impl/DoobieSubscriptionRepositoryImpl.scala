package repository.impl

import cats.effect.Sync
import domain.category._
import domain.supplier._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import doobie.refined.implicits._
import domain.subscription._
import repository.SubscriptionRepository
import repository.impl.logger.logger._

import java.util.UUID

class DoobieSubscriptionRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends SubscriptionRepository[F] {

  private val subscribeCategoryQuery = fr"INSERT INTO category_subscription VALUES "
  private val subscribeSupplierQuery = fr"INSERT INTO supplier_subscription VALUES "
  private val checkSubscriptionQuery = fr"SELECT 1 WHERE NOT EXISTS ( SELECT 1 FROM "
  private val removeCategorySubQuery = fr"DELETE FROM category_subscription "
  private val removeSupplierSubQuery = fr"DELETE FROM supplier_subscription "
  private val getCategorySubQuery    = fr"SELECT category_id FROM category_subscription "
  private val getSupplierSubQuery =
    fr"SELECT s.id,s.name,s.address FROM supplier AS s INNER JOIN supplier_subscription AS ss ON s.id=ss.supplier_id "

  override def subscribeCategory(category: CategorySubscription): F[Int] = {
    (subscribeCategoryQuery ++ fr"(${category.category}, ${category.userId}::UUID)").update.run
      .transact(tx)
  }

  override def subscribeSupplier(supplier: SupplierSubscription): F[Int] =
    (subscribeSupplierQuery ++ fr"(${supplier.supplierId}, ${supplier.userId}::UUID)").update.run
      .transact(tx)

  override def checkCategorySubscription(category: CategorySubscription): F[Option[Int]] = {
    (checkSubscriptionQuery ++ fr"category_subscription WHERE user_id=${category.userId}::UUID AND category_id=${category.category})")
      .query[Int]
      .option
      .transact(tx)
  }

  override def checkSupplierSubscription(supplier: SupplierSubscription): F[Option[Int]] = {
    (checkSubscriptionQuery ++ fr"supplier_subscription WHERE user_id=${supplier.userId}::UUID AND supplier_id=${supplier.supplierId})")
      .query[Int]
      .option
      .transact(tx)
  }

  override def removeSupplierSubscription(supplier: SupplierSubscription): F[Int] = {
    (removeSupplierSubQuery ++ fr"supplier_subscription WHERE user_id=${supplier.userId}::UUID AND supplier_id=${supplier.supplierId})").update.run
      .transact(tx)
  }

  override def removeCategorySubscription(category: CategorySubscription): F[Int] = {
    (removeCategorySubQuery ++ fr"category_subscription WHERE user_id=${category.userId}::UUID AND category_id=${category.category})").update.run
      .transact(tx)
  }

  override def getSupplierSubscriptions(id: UUID): F[List[Supplier]] = {
    (getSupplierSubQuery ++ fr" WHERE ss.user_id = $id").query[Supplier].to[List].transact(tx)
  }

  override def getCategorySubscriptions(id: UUID): F[List[Category]] = {
    (getCategorySubQuery ++ fr" WHERE user_id = $id").query[Category].to[List].transact(tx)
  }
}
