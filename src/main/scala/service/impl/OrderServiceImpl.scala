package service.impl

import cats.Monad
import cats.data.{Chain, EitherT, NonEmptyList}
import cats.syntax.all._
import domain.order.OrderStatus
import domain.product.ProductStatus
import domain.user.AuthorizedUserDomain
import dto.order._
import logger.LogHandler
import repository.{OrderRepository, ProductRepository}
import service.OrderService
import service.error.general.GeneralError
import service.error.order.OrderError._
import util.ConvertToErrorsUtil._
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}
import util.ModelMapper.DomainToDto._
import util.ModelMapper.DtoToDomain._
import util.UpdateOrderStatusRule.checkCurrentStatus

import java.util.UUID

class OrderServiceImpl[F[_]: Monad](
  orderRepository:   OrderRepository[F],
  productRepository: ProductRepository[F],
  logHandler:        LogHandler[F]
) extends OrderService[F] {
  override def viewActiveOrders(user: AuthorizedUserDomain): F[List[OrderReadDto]] = {
    for {
      orders <- orderRepository.viewActiveOrders(user)
      _      <- logHandler.debug(s"Found some orders : $orders")
    } yield orders.map(readOrderDomainToDto)
  }

  override def createOrder(user: AuthorizedUserDomain, createDto: OrderCreateDto): F[ErrorsOr[UUID]] = {
    val res = for {
      _                 <- logHandler.debug(s"Start validation : OrderCreateDto").toErrorsOr
      domain            <- validateCreateOrderDto(createDto).toErrorsOr(fromValidatedNec)
      _                 <- logHandler.debug(s"Validation finished : OrderCreateDto").toErrorsOr
      givenIds           = domain.orderItems.map(_.productId)
      availableProducts <- productRepository.viewProducts(user, NonEmptyList.of(ProductStatus.Available)).toErrorsOr
      _                 <- logHandler.debug(s"Available products : $availableProducts").toErrorsOr
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
      _  <- logHandler.debug(s"Order total price : $total").toErrorsOr
      id <- orderRepository.createOrder(user, domain.copy(total = total)).toErrorsOr
      _  <- logHandler.debug(s"Order created, id = $id").toErrorsOr
    } yield id

    res.value
  }

  override def cancelOrder(user: AuthorizedUserDomain, id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      order <- orderRepository.getById(id).toErrorsOr
      curOrder <- EitherT.fromOption(
        order.flatMap(o => Option.when(o.userId == user.id)(o)),
        Chain[GeneralError](OrderNotFound(id.toString))
      )
      _     <- logHandler.debug(s"Order found : ${curOrder.id}").toErrorsOr
      _     <- EitherT.fromEither(checkCurrentStatus(curOrder.orderStatus, OrderStatus.Cancelled))
      count <- orderRepository.cancelOrder(id).toErrorsOr
      _     <- logHandler.debug(s"Order status updated").toErrorsOr
    } yield count

    res.value
  }
}
