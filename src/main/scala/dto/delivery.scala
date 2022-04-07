package dto

import dto.order.ReadOrderDto
import dto.user.ReadAuthorizedUserDto

object delivery {

  final case class CreateDeliveryDto(
    courierId: String,
    orderId:   String
  )

  final case class ReadDeliveryDto(
    id:                 String,
    order:              String,
    courier:            ReadAuthorizedUserDto,
    deliveryStartDate:  String,
    deliveryFinishDate: Option[String]
  )
}
