package dto

object attachment {

  final case class CreateAttachmentDto(
    attachment: String,
    productId:  String
  )
}
