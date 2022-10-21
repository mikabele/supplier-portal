package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import dto.group.{GroupCreateDto, GroupReadDto, GroupWithProductsDto, GroupWithUsersDto}
import error.general.GeneralError
import error.group.ProductGroupError.{
  GroupExists,
  ProductAlreadyInGroup,
  ProductGroupNotFound,
  ProductIsNotInGroup,
  UserAlreadyInGroup,
  UserIsNotInGroup
}
import error.product.ProductError.ProductNotFound
import error.user.UserError.UserNotFound
import logger.LogHandler
import repository.{ProductGroupRepository, ProductRepository, UserRepository}
import service.ProductGroupService
import util.ConvertToErrorsUtil.ErrorsOr
import util.ModelMapper.DomainToDto.readProductGroupDomainToDto
import util.ConvertToErrorsUtil._
import util.ConvertToErrorsUtil.instances._
import util.ModelMapper.DtoToDomain.{
  validateCreateProductGroupDto,
  validateProductGroupWithProductsDto,
  validateProductGroupWithUsersDto
}

import java.util.UUID

class ProductGroupServiceImpl[F[_]: Monad](
  productGroupRepository: ProductGroupRepository[F],
  userRepository:         UserRepository[F],
  productRepository:      ProductRepository[F],
  logHandler:             LogHandler[F]
) extends ProductGroupService[F] {
  override def showGroups(): F[List[GroupReadDto]] = {
    for {
      groups <- productGroupRepository.showGroups()
      _      <- logHandler.debug(s"Found some groups : $groups")
    } yield groups.map(readProductGroupDomainToDto)
  }

  override def deleteGroup(id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      count <- productGroupRepository.deleteGroup(id).toErrorsOr
      _     <- EitherT.cond(count > 0, (), Chain[GeneralError](ProductGroupNotFound(id.toString)))
      _     <- logHandler.debug(s"Group deleted").toErrorsOr
    } yield count

    res.value
  }

  override def removeUsers(groupWithUsers: GroupWithUsersDto): F[ErrorsOr[Int]] = {
    val res = for {
      _      <- logHandler.debug(s"Start validation : GroupWithUsersDto").toErrorsOr
      domain <- validateProductGroupWithUsersDto(groupWithUsers).toErrorsOr(fromValidatedNec)
      _      <- logHandler.debug(s"Validation finished: GroupWithUsersDto").toErrorsOr
      group <- EitherT.fromOptionF(
        productGroupRepository.getById(UUID.fromString(domain.id.value)),
        Chain[GeneralError](ProductGroupNotFound(domain.id.toString))
      )
      _             <- logHandler.debug(s"Group found : ${group.id}").toErrorsOr
      invalidUserIds = domain.userIds.toList.diff(group.userIds)
      _ <- EitherT.cond(
        invalidUserIds.isEmpty,
        (),
        Chain.fromSeq(invalidUserIds.map(id => UserIsNotInGroup(id.value, group.id.value)))
      )
      count <- productGroupRepository.removeUsers(domain).toErrorsOr
      _     <- logHandler.debug(s"Users removed").toErrorsOr
    } yield count

    res.value
  }

  override def removeProducts(groupWithProducts: GroupWithProductsDto): F[ErrorsOr[Int]] = {
    val res = for {
      _      <- logHandler.debug(s"Start validation : GroupWithProductsDto").toErrorsOr
      domain <- validateProductGroupWithProductsDto(groupWithProducts).toErrorsOr(fromValidatedNec)
      _      <- logHandler.debug(s"Validation finished : GroupWithUsersDto").toErrorsOr
      group <- EitherT.fromOptionF(
        productGroupRepository.getById(UUID.fromString(domain.id.value)),
        Chain[GeneralError](ProductGroupNotFound(domain.id.toString))
      )
      _                <- logHandler.debug(s"Group found : ${group.id}").toErrorsOr
      invalidProductIds = domain.productIds.toList.diff(group.productIds)
      _ <- EitherT.cond(
        invalidProductIds.isEmpty,
        (),
        Chain.fromSeq[GeneralError](invalidProductIds.map(id => ProductIsNotInGroup(id.value, domain.id.value)))
      )
      count <- productGroupRepository.removeProducts(domain).toErrorsOr
      _     <- logHandler.debug(s"Products removed").toErrorsOr
    } yield count

    res.value
  }

  override def addUsers(groupWithUsers: GroupWithUsersDto): F[ErrorsOr[Int]] = {
    val res = for {
      _      <- logHandler.debug(s"Start validation : GroupWithUsersDto").toErrorsOr
      domain <- validateProductGroupWithUsersDto(groupWithUsers).toErrorsOr(fromValidatedNec)
      _      <- logHandler.debug(s"Validation finished: GroupWithUsersDto").toErrorsOr
      group <- EitherT.fromOptionF(
        productGroupRepository.getById(UUID.fromString(domain.id.value)),
        Chain[GeneralError](ProductGroupNotFound(domain.id.toString))
      )
      _             <- logHandler.debug(s"Group found : ${group.id}").toErrorsOr
      users         <- userRepository.getByIds(domain.userIds.map(_.value)).toErrorsOr
      _             <- logHandler.debug(s"Users in DB : $users").toErrorsOr
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
      _     <- logHandler.debug(s"Users added to group").toErrorsOr
    } yield count

    res.value
  }

  override def addProducts(groupWithProducts: GroupWithProductsDto): F[ErrorsOr[Int]] = {
    val res = for {
      _      <- logHandler.debug(s"Start validation : GroupWithProductsDto").toErrorsOr
      domain <- validateProductGroupWithProductsDto(groupWithProducts).toErrorsOr(fromValidatedNec)
      _      <- logHandler.debug(s"Validation finished: GroupWithProductsDto").toErrorsOr
      group <- EitherT.fromOptionF(
        productGroupRepository.getById(UUID.fromString(domain.id.value)),
        Chain[GeneralError](ProductGroupNotFound(domain.id.toString))
      )
      _                <- logHandler.debug(s"Group found : ${group.id}").toErrorsOr
      products         <- productRepository.getByIds(domain.productIds.map(id => UUID.fromString(id.value))).toErrorsOr
      _                <- logHandler.debug(s"Users in DB : $products").toErrorsOr
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
      _     <- logHandler.debug(s"Products added to group").toErrorsOr
    } yield count

    res.value
  }

  override def createGroup(group: GroupCreateDto): F[ErrorsOr[UUID]] = {
    val res = for {
      _      <- logHandler.debug(s"Start validation : GroupCreateDto").toErrorsOr
      domain <- validateCreateProductGroupDto(group).toErrorsOr(fromValidatedNec)
      _      <- logHandler.debug(s"Validation finished: GroupCreateDto").toErrorsOr
      check  <- productGroupRepository.checkByName(group.name).toErrorsOr
      _      <- EitherT.cond(check.isEmpty, (), Chain[GeneralError](GroupExists))
      _      <- logHandler.debug(s"Group has unique name").toErrorsOr
      id     <- productGroupRepository.addGroup(domain).toErrorsOr
      _      <- logHandler.debug(s"Group created").toErrorsOr
    } yield id

    res.value
  }
}
