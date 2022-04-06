package service.impl

import cats.Monad
import cats.data.{Chain, EitherT, NonEmptyList}
import cats.syntax.all._
import domain.product.ProductStatus
import dto.order._
import repository.{OrderRepository, ProductRepository}
import service.OrderService
import service.error.general.{ErrorsOr, GeneralError}
import service.error.order.OrderError.{DuplicatedProductInOrder, OrderNotFound, ProductIsNotAvailable}
import service.error.product.ProductError.ProductNotFound
import util.ModelMapper._

import java.util.UUID

class OrderServiceImpl[F[_]: Monad](orderRepository: OrderRepository[F], productRepository: ProductRepository[F])
  extends OrderService[F] {
  override def viewActiveOrders(): F[List[ReadOrderDto]] = {
    for {
      orders <- orderRepository.viewActiveOrders()
    } yield orders.map(readOrderDomainToDto)
  }

  override def updateOrder(updateDto: UpdateOrderDto): F[ErrorsOr[UpdateOrderDto]] = {
    val res = for {
      domain <- EitherT.fromEither(validateUpdateOrderDto(updateDto).toEither.leftMap(_.toChain))
      count  <- EitherT.liftF(orderRepository.updateOrder(domain)).leftMap((_: Nothing) => Chain.empty[GeneralError])
      _      <- EitherT.cond(count > 0, count, Chain[GeneralError](OrderNotFound(domain.id)))
    } yield updateOrderDomainToDto(domain)

    res.value
  }

  override def createOrder(createDto: CreateOrderDto): F[ErrorsOr[UUID]] = {
    val res = for {
      domain       <- EitherT.fromEither(validateCreateOrderDto(createDto).toEither.leftMap(_.toChain))
      givenIds      = domain.orderItems.map(_.productId)
      duplicatedIds = givenIds.diff(givenIds.distinct).distinct
      _ <- EitherT.cond(
        duplicatedIds.isEmpty,
        (),
        Chain.fromSeq[GeneralError](duplicatedIds.map(DuplicatedProductInOrder))
      )
      availableProducts <- EitherT
        .liftF(productRepository.viewProducts(NonEmptyList.of(ProductStatus.Available)))
        .leftMap((_: Nothing) => Chain.empty[GeneralError])
      availableIds    = availableProducts.map(_.id)
      notAvailableIds = givenIds.diff(availableIds)
      _ <- EitherT.cond(
        notAvailableIds.isEmpty,
        (),
        Chain.fromSeq[GeneralError](notAvailableIds.map(id => ProductIsNotAvailable(id)))
      )

      total = availableProducts
        .filter(p => givenIds.contains(p.id))
        .sortBy(_.id.value)
        .zip(domain.orderItems.sortBy(_.productId.value))
        .map(t => {
          val (product, item) = t
          product.price.value * item.count.value
        })
        .sum
      id <- EitherT
        .liftF(orderRepository.createOrder(domain.copy(total = total)))
        .leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield id

    res.value
  }
}
