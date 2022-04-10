package service

import cats.effect.kernel.Sync
import dto.order._
import repository.{OrderRepository, ProductRepository}
import service.impl.OrderServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

import java.util.UUID

trait OrderService[F[_]] {
  def cancelOrder(userId: UUID, id: UUID): F[ErrorsOr[Int]]

  def viewActiveOrders(userId: UUID): F[List[OrderReadDto]]

  def createOrder(userId: UUID, createDto: OrderCreateDto): F[ErrorsOr[UUID]]
}

object OrderService {
  def of[F[_]: Sync](orderRepository: OrderRepository[F], productRepository: ProductRepository[F]): OrderService[F] = {
    new OrderServiceImpl[F](orderRepository, productRepository)
  }
}
