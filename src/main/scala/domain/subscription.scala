package domain

import domain.category.Category
import types._

object subscription {

  final case class SupplierSubscriptionDomain(
    supplierId: PositiveInt
  )

  final case class CategorySubscriptionDomain(
    category: Category
  )
}
