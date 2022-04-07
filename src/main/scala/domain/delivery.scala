package domain

import domain.user.ReadAuthorizedUser
import types._
import order._

object delivery {

  final case class CreateDelivery(
    courierId: UuidStr,
    orderId:   UuidStr
  )

  final case class ReadDelivery(
    id:                 UuidStr,
    orderId:            UuidStr,
    courier:            ReadAuthorizedUser,
    deliveryStartDate:  DateStr,
    deliveryFinishDate: Option[DateStr]
  )
}
