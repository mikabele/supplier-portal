package dto

object group {

  final case class GroupDto(
    id:         String,
    userIds:    List[String],
    productIds: List[String]
  )
}
