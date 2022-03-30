package domain

import types._
import order._
import courier._

object delivery {
  final case class Delivery(
    id:                 UuidStr,
    order:              Order,
    courier:            Courier,
    address:            NonEmptyStr,
    status:             DeliveryStatus,
    deliveryStartDate:  DateStr,
    deliveryFinishDate: DateStr
  )

  sealed trait DeliveryStatus
  object DeliveryStatus {
    final case object Ordered extends DeliveryStatus
    final case object OnTheWay extends DeliveryStatus
    final case object Delivered extends DeliveryStatus
  }
}
