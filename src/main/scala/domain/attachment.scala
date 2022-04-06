package domain

import types._

object attachment {

  final case class CreateAttachment(
    attachment: UrlStr,
    productId:  UuidStr
  )

  final case class ReadAttachment(
    id:         UuidStr,
    attachment: UrlStr
  )
}
