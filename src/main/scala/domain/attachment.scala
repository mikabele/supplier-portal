package domain

import types._

object attachment {
  // TODO - maybe change type of attachment to BLOB

  final case class CreateAttachment(
    attachment: UrlStr,
    productId:  UuidStr
  )
}
