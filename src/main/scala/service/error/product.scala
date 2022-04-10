package service.error

import service.error.general.{GeneralError, NotFoundError}

object product {
  object ProductError {
    final case class ProductNotFound(id: String) extends NotFoundError {
      override def message: String = s"Product with id $id doesn't exist"
    }
  }
}
