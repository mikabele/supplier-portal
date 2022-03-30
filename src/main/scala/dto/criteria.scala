package dto

object criteria {
  // TODO - think how to make some criterias to be optional

  final case class CriteriaDto(
    name:              Option[String],
    categoryId:        Option[String],
    description:       Option[String],
    supplierId:        Option[String],
    publicationPeriod: Option[String]
  )
}
