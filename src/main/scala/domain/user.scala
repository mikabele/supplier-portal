package domain

import types._

object user {

  sealed trait Role
  object Role {
    final case object Manager extends Role
    final case object Client extends Role
    final case object Courier extends Role
  }

  final case class AuthorizedUser(
    userID:   Int,
    login:    NonEmptyStr,
    password: NonEmptyStr,
    role:     Role,
    phone:    PhoneStr,
    email:    EmailStr
  )
}
