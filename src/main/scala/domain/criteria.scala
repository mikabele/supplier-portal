package domain

import domain.product.ProductStatus
import types._

object criteria {

  final case class CriteriaDomain(
    name:         Option[String]           = None,
    categoryName: Option[String]           = None,
    description:  Option[String]           = None,
    supplierName: Option[String]           = None,
    status:       Option[ProductStatus]    = None,
    minPrice:     Option[NonNegativeFloat] = None,
    maxPrice:     Option[NonNegativeFloat] = None,
    startDate:    Option[DateStr]          = None,
    endDate:      Option[DateStr]          = None
  )
}
