package domain

import user._
import types._
import supplier._
import category._

object subscription {

  final case class SupplierSubscription(
    user:      AuthorizedUser,
    suppliers: List[Supplier],
    startDate: DateStr
  )

  final case class CategorySubscription(
    user:       AuthorizedUser,
    categories: List[Category],
    startDate:  DateStr
  )
}
