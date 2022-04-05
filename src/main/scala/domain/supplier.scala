package domain

import types._

object supplier {

  final case class Supplier(
    id:      PositiveInt,
    name:    NonEmptyStr,
    address: NonEmptyStr
  )
}
