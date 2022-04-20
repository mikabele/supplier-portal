package error

object general {
  trait GeneralError {
    def message: String
  }

  trait BadRequestError extends GeneralError
  trait ForbiddenError extends GeneralError
  trait NotFoundError extends GeneralError
}
