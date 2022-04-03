package dto

object subscription {
  final case class SupplierSubscriptionDto(
    userId:     String, //temp field
    supplierId: Int
  )

  final case class CategorySubscriptionDto(
    userId:     String, //temp field
    categoryId: Int
  )
}
