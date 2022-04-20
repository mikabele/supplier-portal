package dto

object supplier {

  final case class SupplierDto(
    id:      Int,
    name:    String,
    address: String
  )
}
