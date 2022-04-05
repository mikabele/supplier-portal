package domain

import types._

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
