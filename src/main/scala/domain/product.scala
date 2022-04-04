package domain

import domain.category._
import domain.supplier._
import types._

// TODO - add publicationPeriod field in ReadProduct

object product {

  final case class CreateProduct(
    name:        NonEmptyStr,
    categoryId:  PositiveInt,
    supplierId:  PositiveInt,
    price:       NonNegativeFloat,
    description: Option[String]
  )

  final case class ReadProduct(
    id:          UuidStr,
    name:        NonEmptyStr,
    category:    Category,
    supplier:    Supplier,
    price:       NonNegativeFloat,
    description: String,
    status:      ProductStatus
  )

  final case class UpdateProduct(
    id:          UuidStr,
    name:        NonEmptyStr,
    categoryId:  PositiveInt,
    supplierId:  PositiveInt,
    price:       NonNegativeFloat,
    description: String,
    status:      ProductStatus
  )

  sealed trait ProductStatus
  object ProductStatus {
    final case object InProcessing extends ProductStatus
    final case object Available extends ProductStatus
    final case object NotAvailable extends ProductStatus

    def of(status: String): ProductStatus = status match {
      case "inProcessing" => InProcessing
      case "available"    => Available
      case "notAvailable" => NotAvailable
    }
  }
}
