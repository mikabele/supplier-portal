package domain

import domain.product.ProductStatus
import types._

object criteria {

  final case class Criteria(
    name:         Option[String]        = None,
    categoryName: Option[String]        = None,
    description:  Option[String]        = None,
    supplierName: Option[String]        = None,
    status:       Option[ProductStatus] = None,
    startDate:    Option[DateStr]       = None,
    endDate:      Option[DateStr]       = None
  )
}
