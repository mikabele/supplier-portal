package dto

import domain.category.Category

object subscription {
  final case class SupplierSubscriptionDto(
    userId:     String, //temp field
    supplierId: Int
  )

  final case class CategorySubscriptionDto(
    userId:   String, //temp field
    category: Category
  )
}
