package service

import cats.effect.kernel.Sync
import dto.delivery.{CreateDeliveryDto, ReadDeliveryDto}
import repository.{DeliveryRepository, OrderRepository}
import service.error.general.ErrorsOr
import service.impl.DeliveryServiceImpl

import java.util.UUID

trait DeliveryService[F[_]] {
  def showDeliveries(): F[List[ReadDeliveryDto]]

  def delivered(id:             UUID):              F[ErrorsOr[Int]]
  def createDelivery(createDto: CreateDeliveryDto): F[ErrorsOr[UUID]]
}

object DeliveryService {
  def of[F[_]: Sync](
    deliveryRepository: DeliveryRepository[F],
    orderRepository:    OrderRepository[F]
  ): DeliveryService[F] = {
    new DeliveryServiceImpl[F](deliveryRepository, orderRepository)
  }
}
