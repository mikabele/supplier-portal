package service

import cats.effect.kernel.Sync
import domain.category.Category
import domain.user.ReadAuthorizedUser
import dto.subscription._
import dto.supplier.SupplierDto
import repository.{SubscriptionRepository, SupplierRepository}
import service.impl.SubscriptionServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

import java.util.UUID

// TODO - add sheduler

trait SubscriptionService[F[_]] {
  def getCategorySubscriptions(user:   ReadAuthorizedUser):             F[List[Category]]
  def getSupplierSubscriptions(user:   ReadAuthorizedUser): F[List[SupplierDto]]
  def removeCategorySubscription(user: ReadAuthorizedUser, category: CategorySubscriptionDto): F[ErrorsOr[Int]]
  def removeSupplierSubscription(user: ReadAuthorizedUser, supplier:    SupplierSubscriptionDto): F[ErrorsOr[Int]]
  def subscribeCategory(user:          ReadAuthorizedUser, categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]]
  def subscribeSupplier(user:          ReadAuthorizedUser, supplierDto: SupplierSubscriptionDto): F[ErrorsOr[Int]]
}

object SubscriptionService {
  def of[F[_]: Sync](
    subscriptionRepository: SubscriptionRepository[F],
    supplierRepository:     SupplierRepository[F]
  ): SubscriptionService[F] = {
    new SubscriptionServiceImpl[F](subscriptionRepository, supplierRepository)
  }
}
