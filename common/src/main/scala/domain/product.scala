package domain

import domain.attachment.AttachmentReadDomain
import domain.category.Category
import domain.supplier.SupplierDomain
import doobie.postgres.implicits._
import enumeratum.EnumEntry.Snakecase
import enumeratum._
import io.circe.Json
import io.circe.syntax._
import types._

object product {

  final case class ProductCreateDomain(
    name:        NonEmptyStr,
    category:    Category,
    supplierId:  PositiveInt,
    price:       NonNegativeFloat,
    description: Option[String]
  )

  final case class ProductReadDomain(
    id:              UuidStr,
    name:            NonEmptyStr,
    category:        Category,
    supplier:        SupplierDomain,
    price:           NonNegativeFloat,
    description:     String,
    status:          ProductStatus,
    publicationDate: DateTimeStr,
    attachments:     List[AttachmentReadDomain]
  )

  final case class ProductReadDbDomain(
    id:              UuidStr,
    name:            NonEmptyStr,
    category:        Category,
    supplier:        SupplierDomain,
    price:           NonNegativeFloat,
    description:     String,
    status:          ProductStatus,
    publicationDate: DateTimeStr
  )

  final case class ProductUpdateDomain(
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
