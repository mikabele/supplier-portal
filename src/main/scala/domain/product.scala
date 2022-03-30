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
    description: String
  )
}
