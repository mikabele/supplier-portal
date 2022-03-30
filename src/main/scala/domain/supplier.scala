package domain

import types._

object supplier {

  final case class Supplier(
    id:      UuidStr,
    name:    NonEmptyStr,
    address: NonEmptyStr
  )
}
