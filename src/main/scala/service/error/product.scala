package service.error

import service.error.general.{GeneralError, NotFoundError}

object product {
  trait ProductError extends GeneralError

  object ProductError {
    final case class ProductNotFound(id: String) extends ProductError with NotFoundError {
      override def message: String = s"Product with id $id doesn't exist"
    }
  }
}
