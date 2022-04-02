package service.error

import cats.data.ValidatedNec
import service.error.general.{BadRequestError, GeneralError}

// TODO - add errror details to class constructor

object validation {

  sealed trait ValidationError extends BadRequestError
  object ValidationError {

    final case class InvalidFieldFormat(refinedError: String) extends ValidationError {
      override def message: String = s"Invalid field format - $refinedError"
    }

    def of(message: String): InvalidFieldFormat = InvalidFieldFormat(message)
  }
}
