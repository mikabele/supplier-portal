package util

import cats.data.ValidatedNec
import cats.syntax.all._
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import service.error.validation.ValidationError

object RefinedValidator {
  def refinedValidation[V, R](
    value: V,
    error: ValidationError
  )(
    implicit validator: Validate[V, R]
  ): ValidatedNec[ValidationError, V Refined R] =
    refineV(value)(validator).left.map(_ => error).toValidatedNec
}
