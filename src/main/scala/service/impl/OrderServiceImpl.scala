package service.impl

import cats.Monad
import cats.data.{Chain, EitherT, NonEmptyList}
import cats.syntax.all._
import domain.order.OrderStatus
import domain.product.ProductStatus
import dto.order._
import repository.{OrderRepository, ProductRepository}
import service.OrderService
import service.error.general.{ErrorsOr, GeneralError}
import service.error.order.OrderError._
import util.ModelMapper._

import java.util.UUID

class OrderServiceImpl[F[_]: Monad](orderRepository: OrderRepository[F], productRepository: ProductRepository[F])
  extends OrderService[F] {
  override def viewActiveOrders(): F[List[ReadOrderDto]] = {
    for {
      orders <- orderRepository.viewActiveOrders()
    } yield orders.map(readOrderDomainToDto)
  }

  private def checkCurrentStatus(curStatus: OrderStatus, newStatus: OrderStatus): ErrorsOr[Unit] = {
    val wrongRes = Chain[GeneralError](InvalidStatusToUpdate(curStatus, newStatus)).asLeft[Unit]
    curStatus match {
      case OrderStatus.Delivered                                                                          => wrongRes
      case OrderStatus.Assigned if newStatus != OrderStatus.Delivered                                     => wrongRes
      case OrderStatus.Ordered if newStatus != OrderStatus.Assigned && newStatus != OrderStatus.Cancelled => wrongRes
      case _                                                                                              => ().asRight[Chain[GeneralError]]
    }
  }

  override def createOrder(createDto: CreateOrderDto): F[ErrorsOr[UUID]] = {
    val res = for {
      _            <- EitherT.cond(createDto.orderItems.nonEmpty, (), Chain[GeneralError](EmptyOrder))
      domain       <- EitherT.fromEither(validateCreateOrderDto(createDto).toEither.leftMap(_.toChain))
      givenIds      = domain.orderItems.map(_.productId)
      duplicatedIds = givenIds.diff(givenIds.distinct).distinct
      _ <- EitherT.cond(
        duplicatedIds.isEmpty,
        (),
        Chain.fromSeq[GeneralError](duplicatedIds.map(id => DuplicatedProductInOrder(id.value)))
      )
      availableProducts <- EitherT
        .liftF(productRepository.viewProducts(NonEmptyList.of(ProductStatus.Available)))
        .leftMap((_: Nothing) => Chain.empty[GeneralError])
      availableIds    = availableProducts.map(_.id)
      notAvailableIds = givenIds.diff(availableIds)
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
      id <- EitherT
        .liftF(orderRepository.createOrder(domain.copy(total = total)))
        .leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield id

    res.value
  }

  override def cancelOrder(id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      orders     <- EitherT.liftF(orderRepository.viewActiveOrders()).leftMap((_: Nothing) => Chain.empty[GeneralError])
      curOrderOpt = orders.find(_.id.value == id.toString)
      curOrder   <- EitherT.fromOption(curOrderOpt, Chain[GeneralError](OrderNotFound(id.toString)))
      _          <- EitherT.fromEither(checkCurrentStatus(curOrder.orderStatus, OrderStatus.Cancelled))
      count      <- EitherT.liftF(orderRepository.cancelOrder(id)).leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield count

    res.value
  }
}
