package repository

import cats.effect.kernel.Sync
import domain.category.Category
import domain.subscription._
import domain.supplier.SupplierDomain
import doobie.util.transactor.Transactor
import repository.impl.DoobieSubscriptionRepositoryImpl

import java.util.UUID

trait SubscriptionRepository[F[_]] {
  def getSupplierSubscriptions(id: UUID): F[List[SupplierDomain]]

  def getCategorySubscriptions(id: UUID): F[List[Category]]

  def removeSupplierSubscription(userId: UUID, supplier: SupplierSubscriptionDomain): F[Int]

  def removeCategorySubscription(userId: UUID, category: CategorySubscriptionDomain): F[Int]

  def subscribeCategory(userId:         UUID, category: CategorySubscriptionDomain): F[Int]
  def subscribeSupplier(userId:         UUID, supplier: SupplierSubscriptionDomain): F[Int]
  def checkCategorySubscription(userId: UUID, category: CategorySubscriptionDomain): F[Option[Int]]
  def checkSupplierSubscription(userId: UUID, supplier: SupplierSubscriptionDomain): F[Option[Int]]
}

object SubscriptionRepository {
  def of[F[_]: Sync](tx: Transactor[F]): SubscriptionRepository[F] = {
    new DoobieSubscriptionRepositoryImpl[F](tx)
  }
}
