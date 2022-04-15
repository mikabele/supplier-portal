package dto

import domain.user.Role

object user {
  final case class NonAuthorizedUserDto(
    login:    String,
    password: String
  )

  final case class AuthorizedUserDto(
    id:      String,
    name:    String,
    surname: String,
    role:    Role,
    phone:   String,
    email:   String
  )
}
