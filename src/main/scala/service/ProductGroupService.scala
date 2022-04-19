package service

import cats.effect.Sync
import dto.group.{GroupCreateDto, GroupReadDto, GroupWithProductsDto, GroupWithUsersDto}
import logger.LogHandler
import repository.{ProductGroupRepository, ProductRepository, UserRepository}
import service.impl.ProductGroupServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

import java.util.UUID

trait ProductGroupService[F[_]] {
  def showGroups(): F[List[GroupReadDto]]

  def deleteGroup(id: UUID): F[ErrorsOr[Int]]

  def removeUsers(groupWithUsers: GroupWithUsersDto): F[ErrorsOr[Int]]

  def removeProducts(groupWithProducts: GroupWithProductsDto): F[ErrorsOr[Int]]

  def addUsers(groupWithUsers: GroupWithUsersDto): F[ErrorsOr[Int]]

  def addProducts(groupWithProducts: GroupWithProductsDto): F[ErrorsOr[Int]]

  def createGroup(group: GroupCreateDto): F[ErrorsOr[UUID]]

}

object ProductGroupService {
  def of[F[_]: Sync](
    productGroupRepository: ProductGroupRepository[F],
    userRepository:         UserRepository[F],
    productRepository:      ProductRepository[F],
    logHandler:             LogHandler[F]
  ): ProductGroupService[F] = {
    new ProductGroupServiceImpl[F](productGroupRepository, userRepository, productRepository, logHandler)
  }
}
