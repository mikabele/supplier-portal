package dto

import dto.user.AuthorizedUserDto

object delivery {

  final case class DeliveryCreateDto(
    orderId: String
  )

  final case class DeliveryReadDto(
    id:                 String,
    order:              String,
    courier:            AuthorizedUserDto,
    deliveryStartDate:  String,
    deliveryFinishDate: Option[String]
  )
}
