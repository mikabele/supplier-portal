package service.error

import service.error.general.GeneralError

object product {
  trait ProductError extends GeneralError {
    def message: String
  }

  object ProductError {
    final case class DuplicatedProduct(product: Product) extends ProductError {
      override def message: String = s"Product $product already exists"
    }
  }
}
