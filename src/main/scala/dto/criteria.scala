package dto

object criteria {

  final case class CriteriaDto(
    id:                Option[String],
    name:              Option[String],
    categoryId:        Option[Int],
    description:       Option[String],
    supplierId:        Option[Int],
    publicationPeriod: Option[String]
  )
}
