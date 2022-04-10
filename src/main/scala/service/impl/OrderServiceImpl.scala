package service.impl

import cats.Monad
import cats.data.{Chain, EitherT, NonEmptyList}
import cats.syntax.all._
import domain.order.OrderStatus
import domain.product.ProductStatus
import dto.order._
import repository.{OrderRepository, ProductRepository}
import service.OrderService
import service.error.general.GeneralError
import service.error.order.OrderError._
import util.ConvertToErrorsUtil.{ErrorsOr, _}
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}
import util.ModelMapper.DomainToDto._
import util.ModelMapper.DtoToDomain._
import util.UpdateOrderStatusRule.checkCurrentStatus

import java.util.UUID

class OrderServiceImpl[F[_]: Monad](orderRepository: OrderRepository[F], productRepository: ProductRepository[F])
  extends OrderService[F] {
  override def viewActiveOrders(userId: UUID): F[List[OrderReadDto]] = {
    for {
      orders <- orderRepository.viewActiveOrders(userId)
    } yield orders.map(readOrderDomainToDto)
  }

  override def createOrder(userId: UUID, createDto: OrderCreateDto): F[ErrorsOr[UUID]] = {
    val res = for {
      domain            <- validateCreateOrderDto(createDto).toErrorsOr(fromValidatedNec)
      givenIds           = domain.orderItems.map(_.productId)
      availableProducts <- productRepository.viewProducts(userId, NonEmptyList.of(ProductStatus.Available)).toErrorsOr
      availableIds       = availableProducts.map(_.id)
      notAvailableIds    = givenIds.diff(availableIds)
      _ <- EitherT.cond(
        notAvailableIds.isEmpty,
        (),
        Chain.fromSeq[GeneralError](notAvailableIds.map(id => ProductIsNotAvailable(id.value)))
      )

      total = availableProducts
        .filter(p => givenIds.contains(p.id))
        .sortBy(_.id.value)
        .zip(domain.orderItems.sortBy(_.productId.value))
        .map { case (product, item) =>
          product.price.value * item.count.value
        }
        .sum
      id <- orderRepository.createOrder(userId, domain.copy(total = total)).toErrorsOr
    } yield id

    res.value
  }

  override def cancelOrder(userId: UUID, id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      curOrder <- EitherT.fromOptionF(
        orderRepository.viewActiveOrders(userId).map(_.find(_.id.toString == id.toString)),
        Chain[GeneralError](OrderNotFound(id.toString))
      )
      _     <- EitherT.fromEither(checkCurrentStatus(curOrder.orderStatus, OrderStatus.Cancelled))
      count <- orderRepository.cancelOrder(id).toErrorsOr
    } yield count

    res.value
  }
}
