package service

import cats.effect.kernel.Sync
import domain.category.Category
import dto.subscription._
import dto.supplier.SupplierDto
import repository.{SubscriptionRepository, SupplierRepository}
import service.error.general.ErrorsOr
import service.impl.SubscriptionServiceImpl

import java.util.UUID

trait SubscriptionService[F[_]] {
  def getCategorySubscriptions(id: UUID): F[List[Category]]

  def getSupplierSubscriptions(id: UUID): F[List[SupplierDto]]

  def removeCategorySubscription(category: CategorySubscriptionDto): F[ErrorsOr[Int]]

  def removeSupplierSubscription(supplier: SupplierSubscriptionDto): F[ErrorsOr[Int]]

  def subscribeCategory(categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]]
  def subscribeSupplier(supplierDto: SupplierSubscriptionDto): F[ErrorsOr[Int]]
}

object SubscriptionService {
  def of[F[_]: Sync](
    subscriptionRepository: SubscriptionRepository[F],
    supplierRepository:     SupplierRepository[F]
  ): SubscriptionService[F] = {
    new SubscriptionServiceImpl[F](subscriptionRepository, supplierRepository)
  }
}
