package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import domain.user.AuthorizedUserDomain
import dto.category.CategoryDto
import dto.subscription.{CategorySubscriptionDto, SupplierSubscriptionDto}
import dto.supplier.SupplierDto
import error.category.CategoryError.CategoryNotFound
import error.general.GeneralError
import error.subscription.SubscriptionError.{SubscriptionExists, SubscriptionNotExists}
import error.supplier.SupplierError.SupplierNotFound
import logger.LogHandler
import repository.{CategoryRepository, SubscriptionRepository, SupplierRepository}
import service.SubscriptionService
import util.ConvertToErrorsUtil._
import util.ConvertToErrorsUtil.instances._
import util.ModelMapper.DomainToDto.{categoryDomainToDto, supplierDomainToDto}
import util.ModelMapper.DtoToDomain.{validateCategorySubscriptionDto, validateSupplierSubscriptionDto}

class SubscriptionServiceImpl[F[_]: Monad](
  subscriptionRepository: SubscriptionRepository[F],
  supplierRepository:     SupplierRepository[F],
  categoryRepository:     CategoryRepository[F],
  logHandler:             LogHandler[F]
) extends SubscriptionService[F] {
  override def subscribeCategory(user: AuthorizedUserDomain, categoryDto: CategorySubscriptionDto): F[ErrorsOr[Int]] = {
    val res = for {
      _        <- logHandler.debug(s"Start validation : CategorySubscriptionDto").toErrorsOr
      category <- validateCategorySubscriptionDto(categoryDto).toErrorsOr(fromValidatedNec)
      _        <- logHandler.debug(s"Validation finished : CategorySubscriptionDto").toErrorsOr
      _ <- EitherT.fromOptionF(
        categoryRepository.getById(category.categoryId),
        Chain[GeneralError](CategoryNotFound(category.categoryId.value))
      )
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

  override def getCategorySubscriptions(user: AuthorizedUserDomain): F[List[CategoryDto]] = {
    for {
      categories <- subscriptionRepository.getCategorySubscriptions(user)
      _          <- logHandler.debug(s"Some category subscriptions : $categories")
    } yield categories.map(categoryDomainToDto)
  }

  override def getSupplierSubscriptions(user: AuthorizedUserDomain): F[List[SupplierDto]] = {
    for {
      suppliers <- subscriptionRepository.getSupplierSubscriptions(user)
      _         <- logHandler.debug(s"Some supplier subscriptions : $suppliers")
    } yield suppliers.map(supplierDomainToDto)
  }
}
