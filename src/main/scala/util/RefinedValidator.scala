package util

import cats.data.ValidatedNec
import cats.syntax.all._
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV

object RefinedValidator {
  def refinedValidation[V, E, R](
    value: V,
    error: E
  )(
    implicit validator: Validate[V, R]
  ): ValidatedNec[E, V Refined R] =
    refineV(value)(validator).left.map(_ => error).toValidatedNec
}
