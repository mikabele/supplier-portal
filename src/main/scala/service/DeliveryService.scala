package service

import cats.effect.kernel.Sync
import dto.delivery.{DeliveryCreateDto, DeliveryReadDto}
import repository.{DeliveryRepository, OrderRepository}
import service.error.general.ErrorsOr
import service.impl.DeliveryServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

import java.util.UUID

trait DeliveryService[F[_]] {
  def showDeliveries(): F[List[DeliveryReadDto]]

  def delivered(id:             UUID): F[ErrorsOr[Int]]
  def createDelivery(courierId: UUID, createDto: DeliveryCreateDto): F[ErrorsOr[UUID]]
}

object DeliveryService {
  def of[F[_]: Sync](
    deliveryRepository: DeliveryRepository[F],
    orderRepository:    OrderRepository[F]
  ): DeliveryService[F] = {
    new DeliveryServiceImpl[F](deliveryRepository, orderRepository)
  }
}
