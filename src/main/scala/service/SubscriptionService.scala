package service

import cats.effect.Sync
import domain.category.Category
import domain.user.AuthorizedUserDomain
import dto.subscription._
import dto.supplier.SupplierDto
import repository.{SubscriptionRepository, SupplierRepository}
import service.impl.SubscriptionServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

// TODO - add sheduler

trait SubscriptionService[F[_]] {
  def getCategorySubscriptions(user:   AuthorizedUserDomain):             F[List[Category]]
  def getSupplierSubscriptions(user:   AuthorizedUserDomain): F[List[SupplierDto]]
  def removeCategorySubscription(user: AuthorizedUserDomain, category: CategorySubscriptionDto): F[ErrorsOr[Int]]
  def removeSupplierSubscription(user: AuthorizedUserDomain, supplier:    SupplierSubscriptionDto): F[ErrorsOr[Int]]
  def subscribeCategory(user:          AuthorizedUserDomain, categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]]
  def subscribeSupplier(user:          AuthorizedUserDomain, supplierDto: SupplierSubscriptionDto): F[ErrorsOr[Int]]
}

object SubscriptionService {
  def of[F[_]: Sync](
    subscriptionRepository: SubscriptionRepository[F],
    supplierRepository:     SupplierRepository[F]
  ): SubscriptionService[F] = {
    new SubscriptionServiceImpl[F](subscriptionRepository, supplierRepository)
  }
}
