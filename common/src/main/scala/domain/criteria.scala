package domain

import types._

object criteria {

  final case class CriteriaDomain(
    name:         Option[String]           = None,
    categoryName: Option[String]           = None,
    description:  Option[String]           = None,
    supplierName: Option[String]           = None,
    minPrice:     Option[NonNegativeFloat] = None,
    maxPrice:     Option[NonNegativeFloat] = None,
    startDate:    Option[DateTimeStr]      = None,
    endDate:      Option[DateTimeStr]      = None
  )
}
