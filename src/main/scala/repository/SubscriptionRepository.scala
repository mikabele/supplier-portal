package repository

import cats.effect.kernel.Sync
import domain.subscription._
import doobie.util.transactor.Transactor
import repository.impl.DoobieSubscriptionRepositoryImpl

import java.util.UUID

trait SubscriptionRepository[F[_]] {
  def subscribeCategory(category: CategorySubscription): F[Int]
  def subscribeSupplier(supplier: SupplierSubscription): F[Int]
}

object SubscriptionRepository {
  def of[F[_]: Sync](tx: Transactor[F]): SubscriptionRepository[F] = {
    new DoobieSubscriptionRepositoryImpl[F](tx)
  }
}
