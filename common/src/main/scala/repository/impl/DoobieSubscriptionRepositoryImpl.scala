package repository.impl

import cats.effect.Async
import domain.category.Category
import domain.subscription.{CategorySubscriptionDomain, SupplierSubscriptionDomain}
import domain.supplier.SupplierDomain
import domain.user.AuthorizedUserDomain
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.transactor.Transactor
import repository.SubscriptionRepository
import repository.impl.logger.logger._

class DoobieSubscriptionRepositoryImpl[F[_]: Async](tx: Transactor[F]) extends SubscriptionRepository[F] {

  private val subscribeCategoryQuery = fr"INSERT INTO category_subscription VALUES "
  private val subscribeSupplierQuery = fr"INSERT INTO supplier_subscription VALUES "
  private val checkSubscriptionQuery = fr"SELECT 1 WHERE NOT EXISTS ( SELECT 1 FROM "
  private val removeCategorySubQuery = fr"DELETE FROM category_subscription "
  private val removeSupplierSubQuery = fr"DELETE FROM supplier_subscription "
  private val getCategorySubQuery    = fr"SELECT category_id FROM category_subscription "
  private val getSupplierSubQuery =
    fr"SELECT s.id,s.name,s.address FROM supplier AS s INNER JOIN supplier_subscription AS ss ON s.id=ss.supplier_id "

  private val updateLastNotificationDateQuery = fr"UPDATE last_notification SET last_date = CURRENT_TIMESTAMP"

  override def subscribeCategory(user: AuthorizedUserDomain, category: CategorySubscriptionDomain): F[Int] = {
    (subscribeCategoryQuery ++ fr"(${category.category}, ${user.id}::UUID)").update.run
      .transact(tx)
  }

  override def subscribeSupplier(user: AuthorizedUserDomain, supplier: SupplierSubscriptionDomain): F[Int] =
    (subscribeSupplierQuery ++ fr"(${supplier.supplierId}, ${user.id}::UUID)").update.run
      .transact(tx)

  override def checkCategorySubscription(
    user:     AuthorizedUserDomain,
    category: CategorySubscriptionDomain
  ): F[Option[Int]] = {
    (checkSubscriptionQuery ++ fr"category_subscription WHERE user_id=${user.id}::UUID AND category_id=${category.category})")
      .query[Int]
      .option
      .transact(tx)
  }

  override def checkSupplierSubscription(
    user:     AuthorizedUserDomain,
    supplier: SupplierSubscriptionDomain
  ): F[Option[Int]] = {
    (checkSubscriptionQuery ++ fr"supplier_subscription WHERE user_id=${user.id}::UUID AND supplier_id=${supplier.supplierId})")
      .query[Int]
      .option
      .transact(tx)
  }

  override def removeSupplierSubscription(user: AuthorizedUserDomain, supplier: SupplierSubscriptionDomain): F[Int] = {
    (removeSupplierSubQuery ++ fr" WHERE user_id=${user.id}::UUID AND supplier_id=${supplier.supplierId}").update.run
      .transact(tx)
  }

  override def removeCategorySubscription(user: AuthorizedUserDomain, category: CategorySubscriptionDomain): F[Int] = {
    (removeCategorySubQuery ++ fr" WHERE user_id=${user.id}::UUID AND category_id=${category.category}").update.run
      .transact(tx)
  }

  override def getSupplierSubscriptions(user: AuthorizedUserDomain): F[List[SupplierDomain]] = {
    (getSupplierSubQuery ++ fr" WHERE ss.user_id = ${user.id}::UUID").query[SupplierDomain].to[List].transact(tx)
  }

  override def getCategorySubscriptions(user: AuthorizedUserDomain): F[List[Category]] = {
    (getCategorySubQuery ++ fr" WHERE user_id = ${user.id}::UUID").query[Category].to[List].transact(tx)
  }

  override def updateLastNotificationDate(): F[Int] = {
    updateLastNotificationDateQuery.update.run.transact(tx)
  }
}
