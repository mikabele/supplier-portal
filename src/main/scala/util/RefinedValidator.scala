package util

import cats.data.ValidatedNec
import cats.syntax.all._
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.refineV
import service.error.general.GeneralError
import service.error.validation.ValidationError.InvalidFieldFormat

object RefinedValidator {
  def refinedValidation[V, R](
    value: V
  )(
    implicit validator: Validate[V, R]
  ): ValidatedNec[GeneralError, V Refined R] =
    refineV(value)(validator).left.map(InvalidFieldFormat).toValidatedNec
}
