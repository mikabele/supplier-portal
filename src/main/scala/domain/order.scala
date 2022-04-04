package domain

import types._

// TODO - reimplement OrderStatus and all other enums with Enumeration and doobie support

object order {
  final case class CreateOrder(
    userId:     UuidStr,
    orderItems: List[OrderItem]
  )

  final case class UpdateOrder(
    id:          UuidStr,
    orderStatus: OrderStatus
  )

  final case class ReadOrder(
    id:               UuidStr,
    orderItems:       List[OrderItem],
    orderStatus:      OrderStatus,
    orderedStartDate: DateStr,
    total:            NonNegativeFloat
  )

  sealed trait OrderStatus
  object OrderStatus {
    final case object Canceled extends OrderStatus
    final case object Ordered extends OrderStatus

    def of(status: String): OrderStatus = status match {
      case "canceled" => Canceled
      case "ordered"  => Ordered
    }
  }

  final case class OrderItem(
    productId: UuidStr,
    count:     PositiveInt
  )
}
