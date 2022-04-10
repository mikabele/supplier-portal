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

  override def subscribeCategory(userId: UUID, category: CategorySubscriptionDomain): F[Int] = {
    (subscribeCategoryQuery ++ fr"(${category.category}, $userId::UUID)").update.run
      .transact(tx)
  }

  override def subscribeSupplier(userId: UUID, supplier: SupplierSubscriptionDomain): F[Int] =
    (subscribeSupplierQuery ++ fr"(${supplier.supplierId}, $userId::UUID)").update.run
      .transact(tx)

  override def checkCategorySubscription(userId: UUID, category: CategorySubscriptionDomain): F[Option[Int]] = {
    (checkSubscriptionQuery ++ fr"category_subscription WHERE user_id=$userId::UUID AND category_id=${category.category})")
      .query[Int]
      .option
      .transact(tx)
  }

  override def checkSupplierSubscription(userId: UUID, supplier: SupplierSubscriptionDomain): F[Option[Int]] = {
    (checkSubscriptionQuery ++ fr"supplier_subscription WHERE user_id=$userId::UUID AND supplier_id=${supplier.supplierId})")
      .query[Int]
      .option
      .transact(tx)
  }

  override def removeSupplierSubscription(userId: UUID, supplier: SupplierSubscriptionDomain): F[Int] = {
    (removeSupplierSubQuery ++ fr" WHERE user_id=$userId::UUID AND supplier_id=${supplier.supplierId}").update.run
      .transact(tx)
  }

  override def removeCategorySubscription(userId: UUID, category: CategorySubscriptionDomain): F[Int] = {
    (removeCategorySubQuery ++ fr" WHERE user_id=$userId::UUID AND category_id=${category.category}").update.run
      .transact(tx)
  }

  override def getSupplierSubscriptions(id: UUID): F[List[SupplierDomain]] = {
    (getSupplierSubQuery ++ fr" WHERE ss.user_id = $id").query[SupplierDomain].to[List].transact(tx)
  }

  override def getCategorySubscriptions(id: UUID): F[List[Category]] = {
    (getCategorySubQuery ++ fr" WHERE user_id = $id").query[Category].to[List].transact(tx)
  }
}
