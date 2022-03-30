package service.error

object product {
  trait ProductError {
    def message: String
  }

  object ProductError {
    final case class DuplicatedProduct(product: Product) extends ProductError {
      override def message: String = s"Product $product already exists"
    }

    final case object InvalidIdFormat extends ProductError {
      override def message: String = s"Product id should be positive number"
    }

    final case object InvalidNameFormat extends ProductError {
      override def message: String = "Product name should be non-empty string"
    }

    final case object InvalidPrice extends ProductError {
      override def message: String = "Price should be non=negative float number"
    }
  }
}
