package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import dto.subscription._
import repository.{SubscriptionRepository, SupplierRepository}
import service.SubscriptionService
import service.error.general.{ErrorsOr, GeneralError}
import service.error.subscription.SubscriptionError.SubscriptionExists
import service.error.supplier.SupplierError.SupplierNotFound
import util.ModelMapper._

// TODO - add SQLException handling when user tried to subscribe the same category/supplier twice

class SubscriptionServiceImpl[F[_]: Monad](
  subscriptionRepository: SubscriptionRepository[F],
  supplierRepository:     SupplierRepository[F]
) extends SubscriptionService[F] {
  override def subscribeCategory(categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]] = {
    val res = for {
      category <- EitherT.fromEither(validateCategorySubscriptionDto(categoryDto).toEither.leftMap(_.toChain))
      _ <- EitherT.fromOptionF(
        subscriptionRepository.checkCategorySubscription(category),
        Chain[GeneralError](SubscriptionExists)
      )
      count <- EitherT
        .liftF(subscriptionRepository.subscribeCategory(category))
        .leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield count

    res.value
  }

  override def subscribeSupplier(supplierDto: SupplierSubscriptionDto): F[ErrorsOr[Int]] = {
    val res = for {
      supplierSubscription <- EitherT.fromEither(
        validateSupplierSubscriptionDto(supplierDto).toEither.leftMap(_.toChain)
      )
      _ <- EitherT.fromOptionF(
        supplierRepository.getById(supplierSubscription.supplierId),
        Chain[GeneralError](SupplierNotFound(supplierSubscription.supplierId))
      )
      _ <- EitherT.fromOptionF(
        subscriptionRepository.checkSupplierSubscription(supplierSubscription),
        Chain[GeneralError](SubscriptionExists)
      )
      count <- EitherT
        .liftF(subscriptionRepository.subscribeSupplier(supplierSubscription))
        .leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield count

    res.value
  }
}
