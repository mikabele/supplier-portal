package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import domain.category._
import domain.user.ReadAuthorizedUser
import dto.subscription._
import dto.supplier._
import repository.{SubscriptionRepository, SupplierRepository}
import service.SubscriptionService
import service.error.general.GeneralError
import service.error.subscription.SubscriptionError.{SubscriptionExists, SubscriptionNotExists}
import service.error.supplier.SupplierError.SupplierNotFound
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}
import util.ConvertToErrorsUtil.{ErrorsOr, _}
import util.ModelMapper.DomainToDto._
import util.ModelMapper.DtoToDomain._

class SubscriptionServiceImpl[F[_]: Monad](
  subscriptionRepository: SubscriptionRepository[F],
  supplierRepository:     SupplierRepository[F]
) extends SubscriptionService[F] {
  override def subscribeCategory(user: ReadAuthorizedUser, categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]] = {
    val res = for {
      category <- validateCategorySubscriptionDto(categoryDto).toErrorsOr(fromValidatedNec)
      _ <- EitherT.fromOptionF(
        subscriptionRepository.checkCategorySubscription(user, category),
        Chain[GeneralError](SubscriptionExists)
      )
      count <- subscriptionRepository.subscribeCategory(user, category).toErrorsOr
    } yield count

    res.value
  }

  override def subscribeSupplier(user: ReadAuthorizedUser, supplierDto: SupplierSubscriptionDto): F[ErrorsOr[Int]] = {
    val res = for {
      supplierSubscription <- validateSupplierSubscriptionDto(supplierDto).toErrorsOr(fromValidatedNec)
      _ <- EitherT.fromOptionF(
        supplierRepository.getById(supplierSubscription.supplierId),
        Chain[GeneralError](SupplierNotFound(supplierSubscription.supplierId.value))
      )
      _ <- EitherT.fromOptionF(
        subscriptionRepository.checkSupplierSubscription(user, supplierSubscription),
        Chain[GeneralError](SubscriptionExists)
      )
      count <- subscriptionRepository.subscribeSupplier(user, supplierSubscription).toErrorsOr
    } yield count

    res.value
  }

  override def removeCategorySubscription(
    user:     ReadAuthorizedUser,
    category: CategorySubscriptionDto
  ): F[ErrorsOr[Int]] = {
    {
      val res = for {
        category <- validateCategorySubscriptionDto(category).toErrorsOr(fromValidatedNec)
        sub      <- subscriptionRepository.checkCategorySubscription(user, category).toErrorsOr
        _ <- EitherT.cond(
          sub.isEmpty,
          (),
          Chain[GeneralError](SubscriptionNotExists)
        )
        count <- subscriptionRepository.removeCategorySubscription(user, category).toErrorsOr
      } yield count

      res.value
    }
  }

  override def removeSupplierSubscription(
    user:     ReadAuthorizedUser,
    supplier: SupplierSubscriptionDto
  ): F[ErrorsOr[Int]] = {
    {
      val res = for {
        supplier <- validateSupplierSubscriptionDto(supplier).toErrorsOr(fromValidatedNec)
        sub      <- subscriptionRepository.checkSupplierSubscription(user, supplier).toErrorsOr
        _ <- EitherT.cond(
          sub.isEmpty,
          (),
          Chain[GeneralError](SubscriptionNotExists)
        )
        count <- subscriptionRepository.removeSupplierSubscription(user, supplier).toErrorsOr
      } yield count

      res.value
    }
  }

  override def getCategorySubscriptions(user: ReadAuthorizedUser): F[List[Category]] = {
    for {
      categories <- subscriptionRepository.getCategorySubscriptions(user)
    } yield categories
  }

  override def getSupplierSubscriptions(user: ReadAuthorizedUser): F[List[SupplierDto]] = {
    for {
      suppliers <- subscriptionRepository.getSupplierSubscriptions(user)
    } yield suppliers.map(supplierDomainToDto)
  }
}
