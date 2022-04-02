package service.error

import cats.Show
import cats.data.Chain

object general {
  trait GeneralError {
    def message: String
  }

  trait BadRequestError extends GeneralError
  //trait UnauthorizedError extends GeneralError - doesn't work
  trait ForbiddenError extends GeneralError
  trait NotFoundError extends GeneralError

  implicit def catsShowForError[A <: GeneralError]: Show[A] = (t: GeneralError) => t.message

  type ErrorsOr[A] = Either[Chain[GeneralError], A]
}
