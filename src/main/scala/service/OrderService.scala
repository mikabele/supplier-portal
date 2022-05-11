package service

import cats.effect.Sync
import domain.user.AuthorizedUserDomain
import dto.order.{OrderCreateDto, OrderReadDto}
import logger.LogHandler
import repository.{OrderRepository, ProductRepository}
import service.impl.OrderServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

import java.util.UUID

trait OrderService[F[_]] {
  def cancelOrder(user: AuthorizedUserDomain, id: UUID): F[ErrorsOr[Int]]

  def viewActiveOrders(user: AuthorizedUserDomain): F[List[OrderReadDto]]

  def createOrder(user: AuthorizedUserDomain, createDto: OrderCreateDto): F[ErrorsOr[UUID]]
}

object OrderService {
  def of[F[_]: Sync](
    orderRepository:   OrderRepository[F],
    productRepository: ProductRepository[F]
  )(
    implicit logHandler: LogHandler[F]
  ): OrderService[F] = {
    new OrderServiceImpl[F](orderRepository, productRepository, logHandler)
  }
}
