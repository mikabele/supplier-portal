package service.error

import service.error.general.{GeneralError, NotFoundError}

object user {
  object UserError {
    final case class UserNotFound(id: String) extends NotFoundError {
      override def message: String = s"User with id $id doesn't exists"
    }
  }
}
