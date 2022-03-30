package dto

import dto.supplier.SupplierDto
import dto.category.CategoryDto
import io.circe.generic._
// import io.circe.generic._

import io.circe.syntax._
// import io.circe.syntax._

import org.http4s._, org.http4s.dsl._
// import org.http4s._
// import org.http4s.dsl._

import org.http4s.circe._
// import org.http4s.circe._

object product {

  final case class CreateProductDto(
    name:        String,
    categoryId:  String,
    supplierId:  String,
    price:       Float,
    description: Option[String]
  )

  final case class UpdateProductDto(
    id:          String,
    name:        String,
    categoryId:  String,
    supplierId:  String,
    price:       Float,
    description: Option[String]
  )

  final case class ReadProductDto(
    id:          String,
    name:        String,
    category:    CategoryDto,
    supplier:    SupplierDto,
    price:       Float,
    description: String
  )
}
