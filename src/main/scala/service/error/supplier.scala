package service.error

import service.error.general.{GeneralError, NotFoundError}
import types.PositiveInt

object supplier {
  trait SupplierError extends GeneralError

  object SupplierError {
    final case class SupplierNotFound(id: PositiveInt) extends SupplierError with NotFoundError {
      override def message: String = s"Supplier with id ${id.value} doesn't exist"
    }
  }
}
