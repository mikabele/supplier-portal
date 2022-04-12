package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import domain.order.OrderStatus
import domain.user.ReadAuthorizedUser
import dto.delivery.{DeliveryCreateDto, DeliveryReadDto}
import repository.{DeliveryRepository, OrderRepository}
import service.DeliveryService
import service.error.delivery.DeliveryError.InvalidDeliveryCourier
import service.error.general.GeneralError
import service.error.order.OrderError.OrderNotFound
import util.ConvertToErrorsUtil.{ErrorsOr, _}
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}
import util.ModelMapper.DomainToDto._
import util.ModelMapper.DtoToDomain._
import util.UpdateOrderStatusRule.checkCurrentStatus

import java.util.UUID

class DeliveryServiceImpl[F[_]: Monad](deliveryRepository: DeliveryRepository[F], orderRepository: OrderRepository[F])
  extends DeliveryService[F] {
  override def showDeliveries(): F[List[DeliveryReadDto]] = {
    for {
      res <- deliveryRepository.showDeliveries()
    } yield res.map(readDeliveryDomainToDto)
  }

  override def delivered(courier: ReadAuthorizedUser, id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      order <- EitherT.fromOptionF(orderRepository.getById(id), Chain[GeneralError](OrderNotFound(id.toString)))
      _     <- EitherT.fromEither(checkCurrentStatus(order.orderStatus, OrderStatus.Delivered))
      count <- deliveryRepository.delivered(courier, id).toErrorsOr
      _     <- EitherT.cond(count > 0, (), Chain[GeneralError](InvalidDeliveryCourier))
    } yield count

    res.value
  }

  override def createDelivery(courier: ReadAuthorizedUser, createDto: DeliveryCreateDto): F[ErrorsOr[UUID]] = {
    val res = for {
      domain <- validateCreateDeliveryDto(createDto).toErrorsOr(fromValidatedNec)
      order <- EitherT.fromOptionF(
        orderRepository.getById(UUID.fromString(domain.orderId.value)),
        Chain[GeneralError](OrderNotFound(domain.orderId.value))
      )
      _  <- EitherT.fromEither(checkCurrentStatus(order.orderStatus, OrderStatus.Assigned))
      id <- deliveryRepository.createDelivery(courier, domain).toErrorsOr
    } yield id

    res.value
  }

}
