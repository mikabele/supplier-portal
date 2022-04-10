package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import domain.category._
import dto.subscription._
import dto.supplier._
import repository.{SubscriptionRepository, SupplierRepository}
import service.SubscriptionService
import service.error.general.GeneralError
import service.error.subscription.SubscriptionError.{SubscriptionExists, SubscriptionNotExists}
import service.error.supplier.SupplierError.SupplierNotFound
import util.ConvertToErrorsUtil.{ErrorsOr, _}
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}
import util.ModelMapper.DomainToDto._
import util.ModelMapper.DtoToDomain._

import java.util.UUID

class SubscriptionServiceImpl[F[_]: Monad](
  subscriptionRepository: SubscriptionRepository[F],
  supplierRepository:     SupplierRepository[F]
) extends SubscriptionService[F] {
  override def subscribeCategory(userId: UUID, categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]] = {
    val res = for {
      category <- validateCategorySubscriptionDto(categoryDto).toErrorsOr(fromValidatedNec)
      _ <- EitherT.fromOptionF(
        subscriptionRepository.checkCategorySubscription(userId, category),
        Chain[GeneralError](SubscriptionExists)
      )
      count <- subscriptionRepository.subscribeCategory(userId, category).toErrorsOr
    } yield count

    res.value
  }

  override def subscribeSupplier(userId: UUID, supplierDto: SupplierSubscriptionDto): F[ErrorsOr[Int]] = {
    val res = for {
      supplierSubscription <- validateSupplierSubscriptionDto(supplierDto).toErrorsOr(fromValidatedNec)
      _ <- EitherT.fromOptionF(
        supplierRepository.getById(supplierSubscription.supplierId),
        Chain[GeneralError](SupplierNotFound(supplierSubscription.supplierId.value))
      )
      _ <- EitherT.fromOptionF(
        subscriptionRepository.checkSupplierSubscription(userId, supplierSubscription),
        Chain[GeneralError](SubscriptionExists)
      )
      count <- subscriptionRepository.subscribeSupplier(userId, supplierSubscription).toErrorsOr
    } yield count

    res.value
  }

  override def removeCategorySubscription(userId: UUID, category: CategorySubscriptionDto): F[ErrorsOr[Int]] = {
    {
      val res = for {
        category <- validateCategorySubscriptionDto(category).toErrorsOr(fromValidatedNec)
        sub      <- subscriptionRepository.checkCategorySubscription(userId, category).toErrorsOr
        _ <- EitherT.cond(
          sub.isEmpty,
          (),
          Chain[GeneralError](SubscriptionNotExists)
        )
        count <- subscriptionRepository.removeCategorySubscription(userId, category).toErrorsOr
      } yield count

      res.value
    }
  }

  override def removeSupplierSubscription(userId: UUID, supplier: SupplierSubscriptionDto): F[ErrorsOr[Int]] = {
    {
      val res = for {
        supplier <- validateSupplierSubscriptionDto(supplier).toErrorsOr(fromValidatedNec)
        sub      <- subscriptionRepository.checkSupplierSubscription(userId, supplier).toErrorsOr
        _ <- EitherT.cond(
          sub.isEmpty,
          (),
          Chain[GeneralError](SubscriptionNotExists)
        )
        count <- subscriptionRepository.removeSupplierSubscription(userId, supplier).toErrorsOr
      } yield count

      res.value
    }
  }

  override def getCategorySubscriptions(userId: UUID): F[List[Category]] = {
    for {
      categories <- subscriptionRepository.getCategorySubscriptions(userId)
    } yield categories
  }

  override def getSupplierSubscriptions(userId: UUID): F[List[SupplierDto]] = {
    for {
      suppliers <- subscriptionRepository.getSupplierSubscriptions(userId)
    } yield suppliers.map(supplierDomainToDto)
  }
}
