package service

import cats.Monad
import cats.effect.Sync
import domain.user.AuthorizedUserDomain
import dto.delivery.{DeliveryCreateDto, DeliveryReadDto}
import logger.LogHandler
import repository.{DeliveryRepository, OrderRepository}
import service.impl.DeliveryServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

import java.util.UUID

trait DeliveryService[F[_]] {
  def showDeliveries(): F[List[DeliveryReadDto]]

  def delivered(courier:      AuthorizedUserDomain, id:        UUID):              F[ErrorsOr[Int]]
  def createDelivery(courier: AuthorizedUserDomain, createDto: DeliveryCreateDto): F[ErrorsOr[UUID]]
}

object DeliveryService {
  def of[F[_]: Sync: Monad](
    deliveryRepository: DeliveryRepository[F],
    orderRepository:    OrderRepository[F]
  )(
    implicit logHandler: LogHandler[F]
  ): DeliveryService[F] = {
    new DeliveryServiceImpl[F](deliveryRepository, orderRepository, logHandler)
  }
}
