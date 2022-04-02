package service.error

import service.error.general.GeneralError

import java.util.UUID

object product {
  trait ProductError extends GeneralError {
    def message: String
  }

  object ProductError {
    final case class ProductNotFound(id: UUID) extends ProductError {
      override def message: String = s"Product with $id doesn't exist"
    }
  }
}
