package dto

import io.circe.generic.JsonCodec

object supplier {

  @JsonCodec
  final case class SupplierDto(
    id:      String,
    name:    String,
    address: String
  )
}
