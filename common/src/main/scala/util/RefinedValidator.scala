package util

import cats.data.ValidatedNec
import cats.syntax.all._
import error.general.GeneralError
import error.validation.ValidationError.InvalidFieldFormat
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV

object RefinedValidator {
  def refinedValidation[V, R](
    value: V
  )(
    implicit validator: Validate[V, R]
  ): ValidatedNec[GeneralError, V Refined R] =
    refineV(value)(validator).left.map(InvalidFieldFormat).toValidatedNec
}
