package service

import cats.Monad
import cats.effect.Sync
import domain.user.AuthorizedUserDomain
import dto.category.CategoryDto
import dto.subscription.{CategorySubscriptionDto, SupplierSubscriptionDto}
import dto.supplier.SupplierDto
import logger.LogHandler
import repository.{CategoryRepository, SubscriptionRepository, SupplierRepository}
import service.impl.SubscriptionServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

trait SubscriptionService[F[_]] {
  def getCategorySubscriptions(user:   AuthorizedUserDomain):             F[List[CategoryDto]]
  def getSupplierSubscriptions(user:   AuthorizedUserDomain): F[List[SupplierDto]]
  def removeCategorySubscription(user: AuthorizedUserDomain, category: CategorySubscriptionDto): F[ErrorsOr[Int]]
  def removeSupplierSubscription(user: AuthorizedUserDomain, supplier:    SupplierSubscriptionDto): F[ErrorsOr[Int]]
  def subscribeCategory(user:          AuthorizedUserDomain, categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]]
  def subscribeSupplier(user:          AuthorizedUserDomain, supplierDto: SupplierSubscriptionDto): F[ErrorsOr[Int]]
}

object SubscriptionService {
  def of[F[_]: Sync: Monad](
    subscriptionRepository: SubscriptionRepository[F],
    supplierRepository:     SupplierRepository[F],
    categoryRepository:     CategoryRepository[F]
  )(
    implicit logHandler: LogHandler[F]
  ): SubscriptionService[F] = {
    new SubscriptionServiceImpl[F](subscriptionRepository, supplierRepository, categoryRepository, logHandler)
  }
}
