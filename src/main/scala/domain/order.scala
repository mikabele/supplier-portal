package domain

import doobie.postgres.implicits.pgEnumString
import enumeratum.EnumEntry.Snakecase
import enumeratum.{CirceEnum, DoobieEnum, Enum, EnumEntry}
import io.circe.Json
import io.circe.syntax._
import types._

object order {
  final case class OrderCreateDomain(
    orderItems: List[OrderProductCreateDomain],
    total:      Float,
    address:    NonEmptyStr
  )

  final case class OrderUpdateDomain(
    id:          UuidStr,
    orderStatus: OrderStatus
  )

  final case class OrderReadDomain(
    id:               UuidStr,
    userId:           UuidStr,
    orderItems:       List[OrderProductReadDomain],
    orderStatus:      OrderStatus,
    orderedStartDate: DateStr,
    total:            NonNegativeFloat,
    address:          NonEmptyStr
  )

  final case class OrderReadDbDomain(
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

  final case class OrderProductReadDomain(
    orderId:   UuidStr,
    productId: UuidStr,
    count:     PositiveInt
  )

  final case class OrderProductCreateDomain(
    productId: UuidStr,
    count:     PositiveInt
  )
}
