package service.error

import domain.user.Role
import service.error.general.{ForbiddenError, GeneralError, NotFoundError}

object user {
  object UserError {
    final case class UserNotFound(id: String) extends NotFoundError {
      override def message: String = s"User with id $id doesn't exists"
    }

    final case object InvalidUserOrPassword extends ForbiddenError {
      override def message: String = s"Invalid user or password"
    }

    final case class InvalidUserRole(actual: Role, expectedRoles: List[Role]) extends ForbiddenError {
      override def message: String = s"Invalid user role : actual - $actual , expected roles - ${expectedRoles}"
    }
  }
}
