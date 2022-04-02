package domain

import cats.data.Validated.Valid
import doobie.Read
import dto.supplier.SupplierDto
import types._
import util.ModelMapper._

object supplier {

  implicit def supplierRead: Read[Supplier] = Read[(Int, String, String)]
    .map { case (id, name, address) =>
      val dto = SupplierDto(id, name, address)
      validateSupplierDto(dto)
    }
    .map { case Valid(a) => a }

  final case class Supplier(
    id:      PositiveInt,
    name:    NonEmptyStr,
    address: NonEmptyStr
  )
}
