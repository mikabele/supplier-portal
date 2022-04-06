package domain

import domain.attachment.ReadAttachment
import domain.category._
import domain.supplier._
import doobie.postgres.implicits._
import enumeratum.EnumEntry.Snakecase
import types._
import enumeratum._
import io.circe.Json
import io.circe.syntax._

object product {

  final case class CreateProduct(
    name:        NonEmptyStr,
    category:    Category,
    supplierId:  PositiveInt,
    price:       NonNegativeFloat,
    description: Option[String]
  )

  final case class ReadProduct(
    id:                UuidStr,
    name:              NonEmptyStr,
    category:          Category,
    supplier:          Supplier,
    price:             NonNegativeFloat,
    description:       String,
    status:            ProductStatus,
    publicationPeriod: DateStr,
    attachments:       List[ReadAttachment]
  )

  final case class UpdateProduct(
    id:          UuidStr,
    name:        NonEmptyStr,
    category:    Category,
    supplierId:  PositiveInt,
    price:       NonNegativeFloat,
    description: String,
    status:      ProductStatus
  )

  sealed trait ProductStatus extends EnumEntry with Snakecase

  case object ProductStatus extends Enum[ProductStatus] with CirceEnum[ProductStatus] with DoobieEnum[ProductStatus] {

    val values: IndexedSeq[ProductStatus] = findValues

    final case object InProcessing extends ProductStatus

    final case object Available extends ProductStatus

    final case object NotAvailable extends ProductStatus

    ProductStatus.values.foreach { status => assert(status.asJson == Json.fromString(status.entryName)) }

    implicit override lazy val enumMeta: doobie.Meta[ProductStatus] =
      pgEnumString("product_status", ProductStatus.withName, _.entryName)
  }
}
