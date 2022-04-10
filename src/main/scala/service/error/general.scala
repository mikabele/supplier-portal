package service.error

import cats.data.Chain

object general {
  trait GeneralError {
    def message: String
  }

  trait BadRequestError extends GeneralError
  trait UnauthorizedError extends GeneralError // - doesn't work
  trait ForbiddenError extends GeneralError
  trait NotFoundError extends GeneralError
}
