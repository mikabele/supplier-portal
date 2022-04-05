package dto

import domain.product.ProductStatus

object criteria {

  final case class CriteriaDto(
    name:         Option[String]        = None,
    categoryName: Option[String]        = None,
    description:  Option[String]        = None,
    supplierName: Option[String]        = None,
    status:       Option[ProductStatus] = None,
    startDate:    Option[String]        = None,
    endDate:      Option[String]        = None
  )
}
