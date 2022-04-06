package service

import cats.effect.kernel.Sync
import dto.order._
import repository.{OrderRepository, ProductRepository}
import service.error.general.ErrorsOr
import service.impl.OrderServiceImpl

import java.util.UUID

trait OrderService[F[_]] {
  def viewActiveOrders(): F[List[ReadOrderDto]]

  def updateOrder(updateDto: UpdateOrderDto): F[ErrorsOr[UpdateOrderDto]]

  def createOrder(createDto: CreateOrderDto): F[ErrorsOr[UUID]]
}

object OrderService {
  def of[F[_]: Sync](orderRepository: OrderRepository[F], productRepository: ProductRepository[F]): OrderService[F] = {
    new OrderServiceImpl[F](orderRepository, productRepository)
  }
}
