package domain

import domain.category.CategoryDomain
import domain.supplier.SupplierDomain
import types._

object subscription {

  final case class SupplierSubscriptionDomain(
    supplierId: PositiveInt
  )

  final case class CategorySubscriptionDomain(
    categoryId: PositiveInt
  )

  final case class SupplierSubscriptionReadDomain(
    clientId: UuidStr,
    supplier: SupplierDomain
  )

  final case class CategorySubscriptionReadDomain(
    clientId: UuidStr,
    category: CategoryDomain
  )
}
