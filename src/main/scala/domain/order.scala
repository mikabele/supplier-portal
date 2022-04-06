package domain

import domain.product.ProductStatus
import doobie.postgres.implicits.pgEnumString
import enumeratum.{CirceEnum, DoobieEnum, Enum, EnumEntry}
import enumeratum.EnumEntry.Snakecase
import io.circe.Json
import types._
import io.circe.syntax._

object order {
  final case class CreateOrder(
    userId:     UuidStr,
    orderItems: List[OrderItem],
    total:      Float
  )

  // TODO - rename class
  final case class CreateOrderItem()

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

  sealed trait OrderStatus extends EnumEntry with Snakecase
  case object OrderStatus extends Enum[OrderStatus] with CirceEnum[OrderStatus] with DoobieEnum[OrderStatus] {
    final case object Canceled extends OrderStatus
    final case object Ordered extends OrderStatus

    val values: IndexedSeq[OrderStatus] = findValues

    OrderStatus.values.foreach { status => assert(status.asJson == Json.fromString(status.entryName)) }

    implicit override lazy val enumMeta: doobie.Meta[OrderStatus] =
      pgEnumString("order_status", OrderStatus.withName, _.entryName)
  }

  final case class OrderItem(
    productId: UuidStr,
    count:     PositiveInt
  )
}
