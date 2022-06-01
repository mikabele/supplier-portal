package dto

object criteria {

  final case class CriteriaDto(
    name:         Option[String] = None,
    categoryName: Option[String] = None,
    description:  Option[String] = None,
    supplierName: Option[String] = None,
    minPrice:     Option[Float]  = None,
    maxPrice:     Option[Float]  = None,
    startDate:    Option[String] = None,
    endDate:      Option[String] = None
  )
}
