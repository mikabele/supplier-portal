package repository

import cats.effect.kernel.Sync
import domain.category.Category
import domain.subscription._
import domain.supplier.SupplierDomain
import domain.user.ReadAuthorizedUser
import doobie.util.transactor.Transactor
import repository.impl.DoobieSubscriptionRepositoryImpl

trait SubscriptionRepository[F[_]] {
  def getSupplierSubscriptions(user: ReadAuthorizedUser): F[List[SupplierDomain]]

  def getCategorySubscriptions(user: ReadAuthorizedUser): F[List[Category]]

  def removeSupplierSubscription(user: ReadAuthorizedUser, supplier: SupplierSubscriptionDomain): F[Int]

  def removeCategorySubscription(user: ReadAuthorizedUser, category: CategorySubscriptionDomain): F[Int]

  def subscribeCategory(user:         ReadAuthorizedUser, category: CategorySubscriptionDomain): F[Int]
  def subscribeSupplier(user:         ReadAuthorizedUser, supplier: SupplierSubscriptionDomain): F[Int]
  def checkCategorySubscription(user: ReadAuthorizedUser, category: CategorySubscriptionDomain): F[Option[Int]]
  def checkSupplierSubscription(user: ReadAuthorizedUser, supplier: SupplierSubscriptionDomain): F[Option[Int]]
}

object SubscriptionRepository {
  def of[F[_]: Sync](tx: Transactor[F]): SubscriptionRepository[F] = {
    new DoobieSubscriptionRepositoryImpl[F](tx)
  }
}
