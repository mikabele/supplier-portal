package domain

import domain.user.ReadAuthorizedUser
import types._
import order._

object delivery {

  final case class DeliveryCreateDomain(
    orderId: UuidStr
  )

  final case class DeliveryReadDomain(
    id:                 UuidStr,
    orderId:            UuidStr,
    courier:            ReadAuthorizedUser,
    deliveryStartDate:  DateStr,
    deliveryFinishDate: Option[DateStr]
  )
}
