package error

import error.general.{BadRequestError, ForbiddenError, NotFoundError}

object product {
  object ProductError {
    final case class ProductNotFound(id: String) extends NotFoundError {
      override def message: String = s"Product with id $id doesn't exist"
    }

    final case class ProductExists(name: String, supplier: Int) extends BadRequestError {
      override def message: String = s"Product with name $name and supplier $supplier already exists"
    }

    final case class DeclineDeleteProduct(count: Int) extends ForbiddenError {
      override def message: String =
        s"You can't delete product, because there are some active orders with this product (count - $count). " +
          s"Please, change status of product to 'in_processing', wait until all orders would be in status 'cancelled' or 'delivered' and try again."
    }
  }
}
