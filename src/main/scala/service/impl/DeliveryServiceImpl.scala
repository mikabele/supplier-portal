package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import domain.order.OrderStatus
import domain.user.AuthorizedUserDomain
import dto.delivery.{DeliveryCreateDto, DeliveryReadDto}
import logger.LogHandler
import repository.{DeliveryRepository, OrderRepository}
import service.DeliveryService
import service.error.delivery.DeliveryError.InvalidDeliveryCourier
import service.error.general.GeneralError
import service.error.order.OrderError.OrderNotFound
import util.ConvertToErrorsUtil._
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}
import util.ModelMapper.DomainToDto._
import util.ModelMapper.DtoToDomain._
import util.UpdateOrderStatusRule.checkCurrentStatus

import java.util.UUID

class DeliveryServiceImpl[F[_]: Monad](
  deliveryRepository: DeliveryRepository[F],
  orderRepository:    OrderRepository[F],
  logHandler:         LogHandler[F]
) extends DeliveryService[F] {
  override def showDeliveries(): F[List[DeliveryReadDto]] = {
    for {
      res <- deliveryRepository.showDeliveries()
      _   <- logHandler.debug(s"Found some deliveries : $res")
    } yield res.map(readDeliveryDomainToDto)
  }

  override def delivered(courier: AuthorizedUserDomain, id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      order <- EitherT.fromOptionF(orderRepository.getById(id), Chain[GeneralError](OrderNotFound(id.toString)))
      _     <- logHandler.debug(s"Order found : ${order.id}").toErrorsOr
      _     <- EitherT.fromEither(checkCurrentStatus(order.orderStatus, OrderStatus.Delivered))
      count <- deliveryRepository.delivered(courier, id).toErrorsOr
      _     <- EitherT.cond(count > 0, (), Chain[GeneralError](InvalidDeliveryCourier))
      _     <- logHandler.debug(s"Order with $id was updated").toErrorsOr
    } yield count

    res.value
  }

  override def createDelivery(courier: AuthorizedUserDomain, createDto: DeliveryCreateDto): F[ErrorsOr[UUID]] = {
    val res = for {
      _      <- logHandler.debug(s"Start validation: DeliveryCreateDto").toErrorsOr
      domain <- validateCreateDeliveryDto(createDto).toErrorsOr(fromValidatedNec)
      _      <- logHandler.debug(s"Validation finished: DeliveryCreateDto").toErrorsOr
      order <- EitherT.fromOptionF(
        orderRepository.getById(UUID.fromString(domain.orderId.value)),
        Chain[GeneralError](OrderNotFound(domain.orderId.value))
      )
      _  <- logHandler.debug(s"Order found : ${order.id}").toErrorsOr
      _  <- EitherT.fromEither(checkCurrentStatus(order.orderStatus, OrderStatus.Assigned))
      id <- deliveryRepository.createDelivery(courier, domain).toErrorsOr
      _  <- logHandler.debug(s"Order with $id was updated").toErrorsOr
    } yield id

    res.value
  }

}
