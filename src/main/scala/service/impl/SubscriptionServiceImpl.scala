package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import domain.category._
import domain.user.AuthorizedUserDomain
import dto.subscription._
import dto.supplier._
import logger.LogHandler
import repository.{SubscriptionRepository, SupplierRepository}
import service.SubscriptionService
import service.error.general.GeneralError
import service.error.subscription.SubscriptionError.{SubscriptionExists, SubscriptionNotExists}
import service.error.supplier.SupplierError.SupplierNotFound
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}
import util.ConvertToErrorsUtil._
import util.ModelMapper.DomainToDto._
import util.ModelMapper.DtoToDomain._

class SubscriptionServiceImpl[F[_]: Monad](
  subscriptionRepository: SubscriptionRepository[F],
  supplierRepository:     SupplierRepository[F],
  logHandler:             LogHandler[F]
) extends SubscriptionService[F] {
  override def subscribeCategory(user: AuthorizedUserDomain, categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]] = {
    val res = for {
      _        <- logHandler.debug(s"Start validation : CategorySubscriptionDto").toErrorsOr
      category <- validateCategorySubscriptionDto(categoryDto).toErrorsOr(fromValidatedNec)
      _        <- logHandler.debug(s"Validation finished : CategorySubscriptionDto").toErrorsOr
      _ <- EitherT.fromOptionF(
        subscriptionRepository.checkCategorySubscription(user, category),
        Chain[GeneralError](SubscriptionExists)
      )
      count <- subscriptionRepository.subscribeCategory(user, category).toErrorsOr
      _     <- logHandler.debug(s"Subscription created").toErrorsOr
    } yield count

    res.value
  }

  override def subscribeSupplier(user: AuthorizedUserDomain, supplierDto: SupplierSubscriptionDto): F[ErrorsOr[Int]] = {
    val res = for {
      _                    <- logHandler.debug(s"Start validation : SupplierSubscriptionDto").toErrorsOr
      supplierSubscription <- validateSupplierSubscriptionDto(supplierDto).toErrorsOr(fromValidatedNec)
      _                    <- logHandler.debug(s"Validation finished : SupplierSubscriptionDto").toErrorsOr
      _ <- EitherT.fromOptionF(
        supplierRepository.getById(supplierSubscription.supplierId),
        Chain[GeneralError](SupplierNotFound(supplierSubscription.supplierId.value))
      )
      _ <- EitherT.fromOptionF(
        subscriptionRepository.checkSupplierSubscription(user, supplierSubscription),
        Chain[GeneralError](SubscriptionExists)
      )
      count <- subscriptionRepository.subscribeSupplier(user, supplierSubscription).toErrorsOr
      _     <- logHandler.debug(s"Subscription created").toErrorsOr
    } yield count

    res.value
  }

  override def removeCategorySubscription(
    user:     AuthorizedUserDomain,
    category: CategorySubscriptionDto
  ): F[ErrorsOr[Int]] = {
    {
      val res = for {
        _        <- logHandler.debug(s"Start validation : CategorySubscriptionDto").toErrorsOr
        category <- validateCategorySubscriptionDto(category).toErrorsOr(fromValidatedNec)
        _        <- logHandler.debug(s"Validation finished : CategorySubscriptionDto").toErrorsOr
        count    <- subscriptionRepository.removeCategorySubscription(user, category).toErrorsOr
        _        <- EitherT.cond(count > 0, (), Chain[GeneralError](SubscriptionNotExists))
        _        <- logHandler.debug(s"Subscription removed").toErrorsOr
      } yield count

      res.value
    }
  }

  override def removeSupplierSubscription(
    user:     AuthorizedUserDomain,
    supplier: SupplierSubscriptionDto
  ): F[ErrorsOr[Int]] = {
    {
      val res = for {
        _        <- logHandler.debug(s"Start validation : SupplierSubscriptionDto").toErrorsOr
        supplier <- validateSupplierSubscriptionDto(supplier).toErrorsOr(fromValidatedNec)
        _        <- logHandler.debug(s"Validation finished : SupplierSubscriptionDto").toErrorsOr
        count    <- subscriptionRepository.removeSupplierSubscription(user, supplier).toErrorsOr
        _        <- EitherT.cond(count > 0, (), Chain[GeneralError](SubscriptionNotExists))
        _        <- logHandler.debug(s"Subscription removed").toErrorsOr
      } yield count

      res.value
    }
  }

  override def getCategorySubscriptions(user: AuthorizedUserDomain): F[List[Category]] = {
    for {
      categories <- subscriptionRepository.getCategorySubscriptions(user)
      _          <- logHandler.debug(s"Some category subscriptions : $categories")
    } yield categories
  }

  override def getSupplierSubscriptions(user: AuthorizedUserDomain): F[List[SupplierDto]] = {
    for {
      suppliers <- subscriptionRepository.getSupplierSubscriptions(user)
      _         <- logHandler.debug(s"Some supplier subscriptions : $suppliers")
    } yield suppliers.map(supplierDomainToDto)
  }
}
