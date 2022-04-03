package repository.impl

import cats.effect.Sync
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.transactor.Transactor
import doobie.refined.implicits._ // never delete this row
import java.util.UUID

import domain.subscription._
import repository.SubscriptionRepository

class DoobieSubscriptionRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends SubscriptionRepository[F] {

  val subscribeCategoryQuery = fr"INSERT INTO category_subscription VALUES "
  val subscribeSupplierQuery = fr"INSERT INTO supplier_subscription VALUES "

  override def subscribeCategory(category: CategorySubscription): F[Int] = {
    (subscribeCategoryQuery ++ fr"(${category.categoryId}, ${category.userId}::UUID)").update.run
      .transact(tx)
  }

  override def subscribeSupplier(supplier: SupplierSubscription): F[Int] =
    (subscribeSupplierQuery ++ fr"(${supplier.supplierId}, ${supplier.userId}::UUID)").update.run
      .transact(tx)
}
