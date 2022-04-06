package controller

import cats.Show
import domain.order.OrderStatus
import domain.product.ProductStatus
import io.circe.Decoder
import service.error.general.GeneralError

package object implicits {
  implicit def catsShowForError[A <: GeneralError]: Show[A] = (t: GeneralError) => t.message

  //implicit val productStatusDecode: Decoder[ProductStatus] = Decoder.decodeString.map(ProductStatus.of)

  //implicit val orderStatusDecode: Decoder[OrderStatus] = Decoder.decodeString.map(OrderStatus.of)
}
