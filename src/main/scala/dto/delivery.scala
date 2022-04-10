package dto

import dto.user.ReadAuthorizedUserDto

object delivery {

  final case class DeliveryCreateDto(
    orderId: String
  )

  final case class DeliveryReadDto(
    id:                 String,
    order:              String,
    courier:            ReadAuthorizedUserDto,
    deliveryStartDate:  String,
    deliveryFinishDate: Option[String]
  )
}
