package error

import error.general.BadRequestError

object validation {

  object ValidationError {

    final case class InvalidFieldFormat(refinedError: String) extends BadRequestError {
      override def message: String = s"Invalid field format - $refinedError"
    }

    final case class InvalidJsonFormat(error: String) extends BadRequestError {
      override def message: String = s"Invalid json format :$error"
    }
  }
}
