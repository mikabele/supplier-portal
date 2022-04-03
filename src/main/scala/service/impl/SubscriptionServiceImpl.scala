package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.implicits.toBifunctorOps
import dto.subscription._
import repository.SubscriptionRepository
import service.SubscriptionService
import service.error.general.{ErrorsOr, GeneralError}
import util.ModelMapper._

import java.util.UUID

class SubscriptionServiceImpl[F[_]: Monad](subscriptionRepository: SubscriptionRepository[F])
  extends SubscriptionService[F] {
  override def subscribeCategory(categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]] = {
    val res = for {
      category <- EitherT.fromEither(validateCategorySubscriptionDto(categoryDto).toEither.leftMap(_.toChain))
      count <- EitherT
        .liftF(subscriptionRepository.subscribeCategory(category))
        .leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield count

    res.value
  }

  override def subscribeSupplier(supplierDto: SupplierSubscriptionDto): F[ErrorsOr[Int]] = {
    val res = for {
      supplier <- EitherT.fromEither(validateSupplierSubscriptionDto(supplierDto).toEither.leftMap(_.toChain))
      count <- EitherT
        .liftF(subscriptionRepository.subscribeSupplier(supplier))
        .leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield count

    res.value
  }
}
