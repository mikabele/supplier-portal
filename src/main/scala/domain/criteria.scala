package domain

import types._

object criteria {
  // TODO - think how to make some criterias to be optional

  final case class Criteria(
    name:              Option[String],
    categoryId:        Option[UuidStr],
    description:       Option[String],
    supplierId:        Option[UuidStr],
    publicationPeriod: Option[DateStr]
  )
}
