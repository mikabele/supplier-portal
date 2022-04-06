package service.error

import io.circe.Json
import service.error.general.{BadRequestError, GeneralError}

object validation {

  sealed trait ValidationError extends GeneralError
  object ValidationError {

    final case class InvalidFieldFormat(refinedError: String) extends ValidationError with BadRequestError {
      override def message: String = s"Invalid field format - $refinedError"
    }

    final case class InvalidJsonFormat(error: String) extends ValidationError with BadRequestError {
      override def message: String = s"Invalid json format :$error"
    }
  }
}
