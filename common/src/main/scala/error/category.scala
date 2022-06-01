package error

import error.general.NotFoundError

object category {
  object CategoryError {
    final case class CategoryNotFound(id: Int) extends NotFoundError {
      override def message: String = s"Category with id $id doesn't exist"
    }
  }
}
