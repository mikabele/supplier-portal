package repository.impl

import cats.effect.Sync
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import doobie.refined.implicits._ // never delete this row

import domain.subscription._
import repository.SubscriptionRepository

class DoobieSubscriptionRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends SubscriptionRepository[F] {

  private val subscribeCategoryQuery = fr"INSERT INTO category_subscription VALUES "
  private val subscribeSupplierQuery = fr"INSERT INTO supplier_subscription VALUES "
  private val checkSubscriptionQuery = fr"SELECT 1 WHERE NOT EXISTS ( SELECT 1 FROM "

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
}
