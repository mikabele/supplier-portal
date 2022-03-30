package dto

import cats.effect.Concurrent
import org.http4s.EntityDecoder
import io.circe.generic.auto._

object attachment {
//  implicit def createAttachmentDtoDecoder[F[_]: Concurrent]: EntityDecoder[F, CreateAttachmentDto] =
//    org.http4s.circe.jsonOf[F, CreateAttachmentDto]

  final case class CreateAttachmentDto(
    attachment: String,
    productId:  String
  )
}
