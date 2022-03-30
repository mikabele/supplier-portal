package domain

import types._
import product._

object attachment {
  // TODO - maybe change type of attachment to BLOB

  final case class Attachment(
    id:         UuidStr,
    attachment: UrlStr,
    product:    Product
  )
}
