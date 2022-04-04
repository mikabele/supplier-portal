package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import dto.order._
import repository.OrderRepository
import service.OrderService
import service.error.general.{ErrorsOr, GeneralError}
import util.ModelMapper._

import java.util.UUID

class OrderServiceImpl[F[_]: Monad](orderRepository: OrderRepository[F]) extends OrderService[F] {
  override def viewActiveOrders(): F[List[ReadOrderDto]] = {
    for {
      orders <- orderRepository.viewActiveOrders()
    } yield orders.map(readOrderDomainToDto)
  }

  override def updateOrder(updateDto: UpdateOrderDto): F[ErrorsOr[UpdateOrderDto]] = {
    val res = for {
      domain <- EitherT.fromEither(validateUpdateOrderDto(updateDto).toEither.leftMap(_.toChain))
      _      <- EitherT.liftF(orderRepository.updateOrder(domain)).leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield updateOrderDomainToDto(domain)

    res.value
  }

  override def createOrder(createDto: CreateOrderDto): F[ErrorsOr[UUID]] = {
    val res = for {
      domain <- EitherT.fromEither(validateCreateOrderDto(createDto).toEither.leftMap(_.toChain))
      id     <- EitherT.liftF(orderRepository.createOrder(domain)).leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield id

    res.value
  }
}
