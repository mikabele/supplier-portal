package domain

import types._
import user._
import product._

object group {
  final case class ProductGroup(
    id:       UuidStr,
    name:     NonEmptyStr,
    users:    List[AuthorizedUser],
    products: List[Product]
  )
}
