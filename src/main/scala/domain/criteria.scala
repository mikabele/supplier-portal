package domain

import types._

object criteria {
  // TODO - think how to make some criterias to be optional

  final case class Criteria(
    id:                Option[UuidStr]     = None,
    name:              Option[String]      = None,
    categoryId:        Option[PositiveInt] = None,
    description:       Option[String]      = None,
    supplierId:        Option[PositiveInt] = None,
    publicationPeriod: Option[DateStr]     = None
  )
}
