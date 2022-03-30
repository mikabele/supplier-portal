package service.error

import domain.user.Role

object user {
  sealed trait UserAuthorizationError {
    def message: String
  }

  object UserAuthorizationError {
    final case object InvalidLogin extends UserAuthorizationError {
      override def message: String = "Can't find user with given login"
    }

    final case class InvalidPassword(login: String) extends UserAuthorizationError {
      override def message: String = s"Given password doesn't correspond user with login=${login}"
    }

    final case class NotEnoughPermissions(role: Role) extends UserAuthorizationError {
      override def message: String = s"Your user don't have enough permissions. Expected role = ${role}"
    }
  }
}
