package dto

object group {

  final case class GroupCreateDto(
    name: String
  )

  final case class GroupWithUsersDto(
    id:      String,
    userIds: List[String]
  )

  final case class GroupWithProductsDto(
    id:         String,
    productIds: List[String]
  )

  final case class GroupReadDto(
    id:         String,
    name:       String,
    userIds:    List[String],
    productIds: List[String]
  )
}
