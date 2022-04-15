package domain

import domain.user.AuthorizedUserDomain
import types._

object delivery {

  final case class DeliveryCreateDomain(
    orderId: UuidStr
  )

  final case class DeliveryReadDomain(
    id:                 UuidStr,
    orderId:            UuidStr,
    courier:            AuthorizedUserDomain,
    deliveryStartDate:  DateTimeStr,
    deliveryFinishDate: Option[DateTimeStr]
  )
}
