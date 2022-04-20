package domain

import types._

object supplier {

  final case class SupplierDomain(
    id:      PositiveInt,
    name:    NonEmptyStr,
    address: NonEmptyStr
  )
}
