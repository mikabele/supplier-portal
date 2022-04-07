package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import domain.category._
import dto.subscription._
import dto.supplier._
import repository.{SubscriptionRepository, SupplierRepository}
import service.SubscriptionService
import service.error.general.{ErrorsOr, GeneralError}
import service.error.subscription.SubscriptionError.{SubscriptionExists, SubscriptionNotExists}
import service.error.supplier.SupplierError.SupplierNotFound
import util.ModelMapper._

import java.util.UUID

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
        Chain[GeneralError](SupplierNotFound(supplierSubscription.supplierId.value))
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

  override def removeCategorySubscription(category: CategorySubscriptionDto): F[ErrorsOr[Int]] = {
    {
      val res = for {
        category <- EitherT.fromEither(validateCategorySubscriptionDto(category).toEither.leftMap(_.toChain))
        sub <- EitherT
          .liftF(subscriptionRepository.checkCategorySubscription(category))
          .leftMap((_: Nothing) => Chain.empty[GeneralError])
        _ <- EitherT.cond(
          sub.isEmpty,
          (),
          Chain[GeneralError](SubscriptionNotExists)
        )
        count <- EitherT
          .liftF(subscriptionRepository.removeCategorySubscription(category))
          .leftMap((_: Nothing) => Chain.empty[GeneralError])
      } yield count

      res.value
    }
  }

  override def removeSupplierSubscription(supplier: SupplierSubscriptionDto): F[ErrorsOr[Int]] = {
    {
      val res = for {
        supplier <- EitherT.fromEither(validateSupplierSubscriptionDto(supplier).toEither.leftMap(_.toChain))
        sub <- EitherT
          .liftF(subscriptionRepository.checkSupplierSubscription(supplier))
          .leftMap((_: Nothing) => Chain.empty[GeneralError])
        _ <- EitherT.cond(
          sub.isEmpty,
          (),
          Chain[GeneralError](SubscriptionNotExists)
        )
        count <- EitherT
          .liftF(subscriptionRepository.removeSupplierSubscription(supplier))
          .leftMap((_: Nothing) => Chain.empty[GeneralError])
      } yield count

      res.value
    }
  }

  override def getCategorySubscriptions(id: UUID): F[List[Category]] = {
    for {
      categories <- subscriptionRepository.getCategorySubscriptions(id)
    } yield categories
  }

  override def getSupplierSubscriptions(id: UUID): F[List[SupplierDto]] = {
    for {
      suppliers <- subscriptionRepository.getSupplierSubscriptions(id)
    } yield suppliers.map(supplierDomainToDto)
  }
}
