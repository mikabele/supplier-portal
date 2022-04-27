package domain

import types._

object subscription {

  final case class SupplierSubscriptionDomain(
    supplierId: PositiveInt
  )

  final case class CategorySubscriptionDomain(
    categoryId: PositiveInt
  )
}
