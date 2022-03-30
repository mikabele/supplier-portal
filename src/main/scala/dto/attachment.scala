package dto

object attachment {
  final case class CreateAttachmentDto(
    id:         String,
    attachment: String,
    productId:  String
  )
}
