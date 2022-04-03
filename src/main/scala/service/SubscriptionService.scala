package service

import cats.effect.kernel.Sync
import dto.subscription._
import repository.SubscriptionRepository
import service.error.general.ErrorsOr
import service.impl.SubscriptionServiceImpl

import java.util.UUID

trait SubscriptionService[F[_]] {
  def subscribeCategory(categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]]
  def subscribeSupplier(supplierDto: SupplierSubscriptionDto): F[ErrorsOr[Int]]
}

object SubscriptionService {
  def of[F[_]: Sync](subscriptionRepository: SubscriptionRepository[F]): SubscriptionService[F] = {
    new SubscriptionServiceImpl[F](subscriptionRepository)
  }
}
