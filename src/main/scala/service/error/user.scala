package service.error

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
  }
}
