package dto

object attachment {

  final case class AttachmentCreateDto(
    attachment: String,
    productId:  String
  )

  final case class AttachmentReadDto(
    id:         String,
    attachment: String
  )
}
