package dto

object subscription {
  final case class SupplierSubscriptionDto(
    supplierId: Int
  )

  final case class CategorySubscriptionDto(
    categoryId: Int
  )
}
