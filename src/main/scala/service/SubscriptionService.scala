package service

import cats.effect.kernel.Sync
import domain.category.Category
import dto.subscription._
import dto.supplier.SupplierDto
import repository.{SubscriptionRepository, SupplierRepository}
import service.impl.SubscriptionServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

import java.util.UUID

// TODO - add sheduler

trait SubscriptionService[F[_]] {
  def getCategorySubscriptions(userId:   UUID):             F[List[Category]]
  def getSupplierSubscriptions(userId:   UUID): F[List[SupplierDto]]
  def removeCategorySubscription(userId: UUID, category: CategorySubscriptionDto): F[ErrorsOr[Int]]
  def removeSupplierSubscription(userId: UUID, supplier:    SupplierSubscriptionDto): F[ErrorsOr[Int]]
  def subscribeCategory(userId:          UUID, categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]]
  def subscribeSupplier(userId:          UUID, supplierDto: SupplierSubscriptionDto): F[ErrorsOr[Int]]
}

object SubscriptionService {
  def of[F[_]: Sync](
    subscriptionRepository: SubscriptionRepository[F],
    supplierRepository:     SupplierRepository[F]
  ): SubscriptionService[F] = {
    new SubscriptionServiceImpl[F](subscriptionRepository, supplierRepository)
  }
}
