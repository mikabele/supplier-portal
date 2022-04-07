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
    orderItems: List[CreateOrderItem],
    total:      Float,
    address:    NonEmptyStr
  )

  final case class UpdateOrder(
    id:          UuidStr,
    orderStatus: OrderStatus
  )

  final case class ReadOrder(
    id:               UuidStr,
    userId:           UuidStr,
    orderItems:       List[ReadOrderItem],
    orderStatus:      OrderStatus,
    orderedStartDate: DateStr,
    total:            NonNegativeFloat,
    address:          NonEmptyStr
  )

  final case class DbReadOrder(
    id:               UuidStr,
    userId:           UuidStr,
    orderStatus:      OrderStatus,
    orderedStartDate: DateStr,
    total:            NonNegativeFloat,
    address:          NonEmptyStr
  )

  sealed trait OrderStatus extends EnumEntry with Snakecase
  case object OrderStatus extends Enum[OrderStatus] with CirceEnum[OrderStatus] with DoobieEnum[OrderStatus] {
    final case object Cancelled extends OrderStatus
    final case object Ordered extends OrderStatus
    final case object Assigned extends OrderStatus
    final case object Delivered extends OrderStatus

    val values: IndexedSeq[OrderStatus] = findValues

    OrderStatus.values.foreach { status => assert(status.asJson == Json.fromString(status.entryName)) }

    implicit override lazy val enumMeta: doobie.Meta[OrderStatus] =
      pgEnumString("order_status", OrderStatus.withName, _.entryName)
  }

  // TODO - rename class
  final case class ReadOrderItem(
    orderId:   UuidStr,
    productId: UuidStr,
    count:     PositiveInt
  )

  final case class CreateOrderItem(
    productId: UuidStr,
    count:     PositiveInt
  )
}
