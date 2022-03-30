package dto

import io.circe.generic.JsonCodec

object category {

  @JsonCodec
  final case class CategoryDto(
    id:   String,
    name: String
  )
}
