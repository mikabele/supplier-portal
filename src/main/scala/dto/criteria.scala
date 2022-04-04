package dto

object criteria {

  final case class CriteriaDto(
    id:                Option[String] = None,
    name:              Option[String] = None,
    categoryId:        Option[Int]    = None,
    description:       Option[String] = None,
    supplierId:        Option[Int]    = None,
    publicationPeriod: Option[String] = None
  )
}
