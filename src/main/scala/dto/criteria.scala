package dto

import cats.effect.Concurrent
import org.http4s.EntityDecoder
import io.circe.generic.auto._

object criteria {
//  implicit def criteriaDtoDecoder[F[_]: Concurrent]: EntityDecoder[F, CriteriaDto] =
//    org.http4s.circe.jsonOf[F, CriteriaDto]

  final case class CriteriaDto(
    name:              Option[String],
    categoryId:        Option[String],
    description:       Option[String],
    supplierId:        Option[String],
    publicationPeriod: Option[String]
  )
}
