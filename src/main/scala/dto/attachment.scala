package dto

object attachment {

  final case class CreateAttachmentDto(
    attachment: String,
    productId:  String
  )

  final case class ReadAttachmentDto(
    id:         String,
    attachment: String
  )
}
