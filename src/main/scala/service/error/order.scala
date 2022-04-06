package service.error

import service.error.general.{BadRequestError, GeneralError, NotFoundError}
import types.UuidStr

object order {
  trait OrderError extends GeneralError

  object OrderError {
    final case class OrderNotFound(id: UuidStr) extends OrderError with NotFoundError {
      override def message: String = s"Order with id ${id.value} doesn't exist"
    }

    final case class ProductIsNotAvailable(id: UuidStr) extends OrderError with BadRequestError {
      override def message: String = s"Product with id ${id.value} doesn't exist or is not available to be ordered"
    }

    final case class DuplicatedProductInOrder(id: UuidStr) extends OrderError with BadRequestError {
      override def message: String = s"Product with ${id.value} was found in the order more than once"
    }
  }
}
