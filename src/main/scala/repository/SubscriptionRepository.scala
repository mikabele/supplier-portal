package repository

import cats.effect.kernel.Sync
import domain.category.Category
import domain.subscription._
import domain.supplier.Supplier
import doobie.util.transactor.Transactor
import repository.impl.DoobieSubscriptionRepositoryImpl

import java.util.UUID

trait SubscriptionRepository[F[_]] {
  def getSupplierSubscriptions(id: UUID): F[List[Supplier]]

  def getCategorySubscriptions(id: UUID): F[List[Category]]

  def removeSupplierSubscription(supplier: SupplierSubscription): F[Int]

  def removeCategorySubscription(category: CategorySubscription): F[Int]

  def subscribeCategory(category: CategorySubscription): F[Int]
  def subscribeSupplier(supplier: SupplierSubscription): F[Int]
  // TODO - remove 2 next methods
  def checkCategorySubscription(category: CategorySubscription): F[Option[Int]]
  def checkSupplierSubscription(supplier: SupplierSubscription): F[Option[Int]]
}

object SubscriptionRepository {
  def of[F[_]: Sync](tx: Transactor[F]): SubscriptionRepository[F] = {
    new DoobieSubscriptionRepositoryImpl[F](tx)
  }
}
