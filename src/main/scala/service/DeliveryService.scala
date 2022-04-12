package service

import cats.effect.kernel.Sync
import domain.user.ReadAuthorizedUser
import dto.delivery.{DeliveryCreateDto, DeliveryReadDto}
import repository.{DeliveryRepository, OrderRepository}
import service.impl.DeliveryServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

import java.util.UUID

trait DeliveryService[F[_]] {
  def showDeliveries(): F[List[DeliveryReadDto]]

  def delivered(courier:      ReadAuthorizedUser, id:        UUID):              F[ErrorsOr[Int]]
  def createDelivery(courier: ReadAuthorizedUser, createDto: DeliveryCreateDto): F[ErrorsOr[UUID]]
}

object DeliveryService {
  def of[F[_]: Sync](
    deliveryRepository: DeliveryRepository[F],
    orderRepository:    OrderRepository[F]
  ): DeliveryService[F] = {
    new DeliveryServiceImpl[F](deliveryRepository, orderRepository)
  }
}
