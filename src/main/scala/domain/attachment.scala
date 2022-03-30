package domain

import types._
import cats.effect.Concurrent
import org.http4s.EntityDecoder
import io.circe.generic.auto._

object attachment {
  // TODO - maybe change type of attachment to BLOB

  final case class CreateAttachment(
    attachment: UrlStr,
    productId:  UuidStr
  )
}
