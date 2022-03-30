package service.error

import cats.data.ValidatedNec
import service.error.general.GeneralError

object validation {

  sealed trait ValidationError extends GeneralError
  object ValidationError {
    type ValidationErrorChainOr[A] = ValidatedNec[ValidationError, A]

    final case class InvalidIdFormat(field: String) extends ValidationError {
      override def message: String = s"Field \'$field\' should have uuid format"
    }

    final case class NegativeField(field: String) extends ValidationError {
      override def message: String = s"Field \'$field\' should be non-negative"
    }

    final case class EmptyField(field: String) extends ValidationError {
      override def message: String = s"Field \'$field\' should be non-empty"
    }

    final case class NonPositiveField(field: String) extends ValidationError {
      override def message: String = s"Field \'$field\' should be positive number"
    }

    final case class InvalidDateFormat(field: String) extends ValidationError {
      override def message: String = s"Field \'$field\' should have correct format of date \'yyyy-MM-dd\'"
    }

    final case class InvalidUrlFormat(field: String) extends ValidationError {
      override def message: String = s"Field \'$field\' should be URL-string"
    }
  }
}
