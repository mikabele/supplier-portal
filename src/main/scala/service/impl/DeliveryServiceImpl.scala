package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import domain.order.OrderStatus
import dto.delivery.{CreateDeliveryDto, ReadDeliveryDto}
import repository.{DeliveryRepository, OrderRepository}
import service.DeliveryService
import service.error.general.{ErrorsOr, GeneralError}
import service.error.order.OrderError.{InvalidStatusToUpdate, OrderNotFound}
import util.ModelMapper._

import java.util.UUID

class DeliveryServiceImpl[F[_]: Monad](deliveryRepository: DeliveryRepository[F], orderRepository: OrderRepository[F])
  extends DeliveryService[F] {
  override def showDeliveries(): F[List[ReadDeliveryDto]] = {
    for {
      res <- deliveryRepository.showDeliveries()
    } yield res.map(readDeliveryDomainToDto)
  }

  override def delivered(id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      orders  <- EitherT.liftF(orderRepository.viewActiveOrders()).leftMap((_: Nothing) => Chain.empty[GeneralError])
      orderOpt = orders.find(_.id.value == id.toString)
      order   <- EitherT.fromOption(orderOpt, Chain[GeneralError](OrderNotFound(id.toString)))
      _       <- EitherT.fromEither(checkCurrentStatus(order.orderStatus, OrderStatus.Delivered))
      count   <- EitherT.liftF(deliveryRepository.delivered(id)).leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield count

    res.value
  }

  override def createDelivery(createDto: CreateDeliveryDto): F[ErrorsOr[UUID]] = {
    val res = for {
      domain <- EitherT.fromEither(validateCreateDeliveryDto(createDto).toEither.leftMap(_.toChain))
      orders <- EitherT.liftF(orderRepository.viewActiveOrders()).leftMap((_: Nothing) => Chain.empty[GeneralError])
      // TODO - move this 3 lines of code in function
      orderOpt = orders.find(_.id == domain.orderId)
      order   <- EitherT.fromOption(orderOpt, Chain[GeneralError](OrderNotFound(domain.orderId.value)))
      _       <- EitherT.fromEither(checkCurrentStatus(order.orderStatus, OrderStatus.Assigned))
      id      <- EitherT.liftF(deliveryRepository.createDelivery(domain)).leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield id

    res.value
  }

  // TODO - think where i need to place this func
  private def checkCurrentStatus(curStatus: OrderStatus, newStatus: OrderStatus): ErrorsOr[Unit] = {
    val wrongRes = Chain[GeneralError](InvalidStatusToUpdate(curStatus, newStatus)).asLeft[Unit]
    curStatus match {
      case OrderStatus.Delivered                                                                          => wrongRes
      case OrderStatus.Assigned if newStatus != OrderStatus.Delivered                                     => wrongRes
      case OrderStatus.Ordered if newStatus != OrderStatus.Assigned && newStatus != OrderStatus.Cancelled => wrongRes
      case _                                                                                              => ().asRight[Chain[GeneralError]]
    }
  }

}
