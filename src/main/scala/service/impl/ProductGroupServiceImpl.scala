package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import dto.group._
import repository.{ProductGroupRepository, ProductRepository, UserRepository}
import service.ProductGroupService
import service.error.general.GeneralError
import service.error.group.ProductGroupError._
import service.error.product.ProductError.ProductNotFound
import service.error.user.UserError.UserNotFound
import util.ConvertToErrorsUtil.{ErrorsOr, ToErrorsOrSyntax}
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}
import util.ModelMapper.DomainToDto._
import util.ModelMapper.DtoToDomain._

import java.util.UUID

class ProductGroupServiceImpl[F[_]: Monad](
  productGroupRepository: ProductGroupRepository[F],
  userRepository:         UserRepository[F],
  productRepository:      ProductRepository[F]
) extends ProductGroupService[F] {
  override def showGroups(): F[List[GroupReadDto]] = {
    for {
      groups <- productGroupRepository.showGroups()
    } yield groups.map(readProductGroupDomainToDto)
  }

  override def deleteGroup(id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      count <- productGroupRepository.deleteGroup(id).toErrorsOr
      _     <- EitherT.cond(count > 0, (), Chain[GeneralError](ProductGroupNotFound(id.toString)))
    } yield count

    res.value
  }

  override def removeUsers(groupWithUsers: GroupWithUsersDto): F[ErrorsOr[Int]] = {
    val res = for {
      domain <- validateProductGroupWithUsersDto(groupWithUsers).toErrorsOr(fromValidatedNec)
      group <- EitherT.fromOptionF(
        productGroupRepository.getById(UUID.fromString(domain.id.value)),
        Chain[GeneralError](ProductGroupNotFound(domain.id.toString))
      )
      invalidUserIds = domain.userIds.toList.diff(group.userIds)
      _ <- EitherT.cond(
        invalidUserIds.isEmpty,
        (),
        Chain.fromSeq(invalidUserIds.map(id => UserIsNotInGroup(id.value, group.id.value)))
      )
      count <- productGroupRepository.removeUsers(domain).toErrorsOr
    } yield count

    res.value
  }

  override def removeProducts(groupWithProducts: GroupWithProductsDto): F[ErrorsOr[Int]] = {
    val res = for {
      domain <- validateProductGroupWithProductsDto(groupWithProducts).toErrorsOr(fromValidatedNec)
      group <- EitherT.fromOptionF(
        productGroupRepository.getById(UUID.fromString(domain.id.value)),
        Chain[GeneralError](ProductGroupNotFound(domain.id.toString))
      )
      invalidProductIds = domain.productIds.toList.diff(group.productIds)
      _ <- EitherT.cond(
        invalidProductIds.isEmpty,
        (),
        Chain.fromSeq[GeneralError](invalidProductIds.map(id => ProductIsNotInGroup(id.value, domain.id.value)))
      )
      count <- productGroupRepository.removeProducts(domain).toErrorsOr
    } yield count

    res.value
  }

  override def addUsers(groupWithUsers: GroupWithUsersDto): F[ErrorsOr[Int]] = {
    val res = for {
      domain <- validateProductGroupWithUsersDto(groupWithUsers).toErrorsOr(fromValidatedNec)
      group <- EitherT.fromOptionF(
        productGroupRepository.getById(UUID.fromString(domain.id.value)),
        Chain[GeneralError](ProductGroupNotFound(domain.id.toString))
      )
      users         <- userRepository.getByIds(domain.userIds.map(_.value)).toErrorsOr
      invalidUserIds = domain.userIds.toList.diff(users.map(_.id))
      _ <- EitherT.cond(
        invalidUserIds.isEmpty,
        (),
        Chain.fromSeq(invalidUserIds.map(id => UserNotFound(id.value)))
      )
      existsUsers = domain.userIds.toList.intersect(group.userIds)
      _ <- EitherT.cond(
        existsUsers.isEmpty,
        (),
        Chain.fromSeq[GeneralError](existsUsers.map(id => UserAlreadyInGroup(id.value)))
      )
      count <- productGroupRepository.addUsers(domain).toErrorsOr
    } yield count

    res.value
  }

  override def addProducts(groupWithProducts: GroupWithProductsDto): F[ErrorsOr[Int]] = {
    val res = for {
      domain <- validateProductGroupWithProductsDto(groupWithProducts).toErrorsOr(fromValidatedNec)
      group <- EitherT.fromOptionF(
        productGroupRepository.getById(UUID.fromString(domain.id.value)),
        Chain[GeneralError](ProductGroupNotFound(domain.id.toString))
      )
      products         <- productRepository.getByIds(domain.productIds).toErrorsOr
      invalidProductIds = domain.productIds.toList.diff(products.map(_.id))
      _ <- EitherT.cond(
        invalidProductIds.isEmpty,
        (),
        Chain.fromSeq[GeneralError](invalidProductIds.map(id => ProductNotFound(id.value)))
      )
      existsProducts = domain.productIds.toList.intersect(group.productIds)
      _ <- EitherT.cond(
        existsProducts.isEmpty,
        (),
        Chain.fromSeq[GeneralError](existsProducts.map(id => ProductAlreadyInGroup(id.value)))
      )
      count <- productGroupRepository.addProducts(domain).toErrorsOr
    } yield count

    res.value
  }

  override def createGroup(group: GroupCreateDto): F[ErrorsOr[UUID]] = {
    val res = for {
      domain <- validateCreateProductGroupDto(group).toErrorsOr(fromValidatedNec)
      id     <- productGroupRepository.addGroup(domain).toErrorsOr
    } yield id

    res.value
  }
}
