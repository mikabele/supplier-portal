package domain

import types._

object attachment {

  final case class AttachmentCreateDomain(
    attachment: UrlStr,
    productId:  UuidStr
  )

  final case class AttachmentReadDomain(
    id:         UuidStr,
    productId:  UuidStr,
    attachment: UrlStr
  )
}
