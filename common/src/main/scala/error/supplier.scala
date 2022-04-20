package error

import error.general.NotFoundError

object supplier {

  object SupplierError {
    final case class SupplierNotFound(id: Int) extends NotFoundError {
      override def message: String = s"Supplier with id $id doesn't exist"
    }
  }
}
