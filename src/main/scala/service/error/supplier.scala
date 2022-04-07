package service.error

import service.error.general.{GeneralError, NotFoundError}

object supplier {
  trait SupplierError extends GeneralError

  object SupplierError {
    final case class SupplierNotFound(id: Int) extends SupplierError with NotFoundError {
      override def message: String = s"Supplier with id $id doesn't exist"
    }
  }
}
