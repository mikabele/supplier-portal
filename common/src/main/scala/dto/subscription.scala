package dto

import domain.category.Category

object subscription {
  final case class SupplierSubscriptionDto(
    supplierId: Int
  )

  final case class CategorySubscriptionDto(
    category: Category
  )
}
