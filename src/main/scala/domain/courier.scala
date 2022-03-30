package domain

import types._

object courier {
  final case class Courier(
    id:    UuidStr,
    name:  NonEmptyStr,
    phone: PhoneStr
  )
}
