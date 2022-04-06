package repository

import cats.effect.kernel.Sync
import domain.subscription._
import doobie.util.transactor.Transactor
import repository.impl.DoobieSubscriptionRepositoryImpl

trait SubscriptionRepository[F[_]] {
  def subscribeCategory(category:         CategorySubscription): F[Int]
  def subscribeSupplier(supplier:         SupplierSubscription): F[Int]
  def checkCategorySubscription(category: CategorySubscription): F[Option[Int]]
  def checkSupplierSubscription(supplier: SupplierSubscription): F[Option[Int]]
}

object SubscriptionRepository {
  def of[F[_]: Sync](tx: Transactor[F]): SubscriptionRepository[F] = {
    new DoobieSubscriptionRepositoryImpl[F](tx)
  }
}
