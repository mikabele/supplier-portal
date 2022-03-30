package dto

object subscription {
  final case class SupplierSubscriptionDto(
    supplierIds: List[String]
  )

  final case class CategorySubscriptionDto(
    categoryIds: List[String]
  )
}
