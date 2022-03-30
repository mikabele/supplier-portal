package domain

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string._
import types._
import supplier._
import category._

object product {

  final case class CreateProduct(
    name:        NonEmptyStr,
    categoryId:  UuidStr,
    supplierId:  UuidStr,
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
    name:        Option[NonEmptyStr],
    categoryId:  Option[UuidStr],
    supplierId:  Option[UuidStr],
    price:       Option[NonNegativeFloat],
    description: Option[String],
    status:      Option[ProductStatus]
  )

  sealed trait ProductStatus
  object ProductStatus {
    final case object InProcessing extends ProductStatus
    final case object Available extends ProductStatus
    final case object NotAvailable extends ProductStatus
  }
}
