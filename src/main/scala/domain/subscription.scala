package domain

import user._
import types._
import supplier._
import category._

object subscription {

  final case class SupplierSubscription(
    userId:     UuidStr,
    supplierId: PositiveInt
  )

  final case class CategorySubscription(
    userId:     UuidStr,
    categoryId: PositiveInt
  )
}
