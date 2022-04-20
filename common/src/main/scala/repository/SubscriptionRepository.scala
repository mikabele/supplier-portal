package repository

import cats.effect.Async
import domain.category.Category
import domain.subscription.{CategorySubscriptionDomain, SupplierSubscriptionDomain}
import domain.supplier.SupplierDomain
import domain.user.AuthorizedUserDomain
import doobie.util.transactor.Transactor
import repository.impl.DoobieSubscriptionRepositoryImpl

trait SubscriptionRepository[F[_]] {
  def updateLastNotificationDate(): F[Int]

  def getSupplierSubscriptions(user: AuthorizedUserDomain): F[List[SupplierDomain]]

  def getCategorySubscriptions(user: AuthorizedUserDomain): F[List[Category]]

  def removeSupplierSubscription(user: AuthorizedUserDomain, supplier: SupplierSubscriptionDomain): F[Int]

  def removeCategorySubscription(user: AuthorizedUserDomain, category: CategorySubscriptionDomain): F[Int]

  def subscribeCategory(user:         AuthorizedUserDomain, category: CategorySubscriptionDomain): F[Int]
  def subscribeSupplier(user:         AuthorizedUserDomain, supplier: SupplierSubscriptionDomain): F[Int]
  def checkCategorySubscription(user: AuthorizedUserDomain, category: CategorySubscriptionDomain): F[Option[Int]]
  def checkSupplierSubscription(user: AuthorizedUserDomain, supplier: SupplierSubscriptionDomain): F[Option[Int]]
}

object SubscriptionRepository {
  def of[F[_]: Async](tx: Transactor[F]): SubscriptionRepository[F] = {
    new DoobieSubscriptionRepositoryImpl[F](tx)
  }
}
