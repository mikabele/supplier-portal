package service.error

import service.error.general.{ForbiddenError, GeneralError, NotFoundError}

object product {
  object ProductError {
    final case class ProductNotFound(id: String) extends NotFoundError {
      override def message: String = s"Product with id $id doesn't exist"
    }

    final case class DeclineDeleteProduct(count: Int) extends ForbiddenError {
      override def message: String =
        s"You can't delete product, because there are some active orders with this product (count - $count). " +
          s"Please, change status of product to 'in_processing', wait until all orders would be in status 'cancelled' or 'delivered' and try again."
    }
  }
}
