package service.error

import service.error.general.ForbiddenError

object auth {
  object AuthError {
    final case class CookieValidationFail(ex: String) extends ForbiddenError {
      override def message: String = s"Cookie validation failed : reason - $ex"
    }

    final case class InvalidCookieToken(token: String) extends ForbiddenError {
      override def message: String = s"Invalid token $token in cookie"
    }
  }
}
