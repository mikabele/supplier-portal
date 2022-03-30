package dto

object user {
  final case class NonAuthorizedUserDto(
    login:    String,
    password: String
  )
}
