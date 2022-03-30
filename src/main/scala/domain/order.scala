package domain

import domain.user.AuthorizedUser
import types._
import product._

object order {
  final case class Order(
    id:               UuidStr,
    user:             AuthorizedUser,
    total:            NonNegativeFloat,
    status:           OrderStatus,
    orderItems:       List[OrderItem],
    orderedStartDate: DateStr
  )

  sealed trait OrderStatus
  object OrderStatus {
    final case object Canceled extends OrderStatus
    final case object Delivered extends OrderStatus
  }

  final case class OrderItem(
    product: Product,
    count:   PositiveInt
  )
}
