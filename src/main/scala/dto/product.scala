package dto

import domain.product.ProductStatus
import dto.supplier.SupplierDto
import dto.category.CategoryDto

import cats.effect.Concurrent
import org.http4s.EntityDecoder
import io.circe.generic.auto._

object product {

//  implicit def createProductDtoDecoder[F[_]: Concurrent]: EntityDecoder[F, CreateProductDto] =
//    org.http4s.circe.jsonOf[F, CreateProductDto]
//
//  implicit def updateProductDtoDecoder[F[_]: Concurrent]: EntityDecoder[F, UpdateProductDto] =
//    org.http4s.circe.jsonOf[F, UpdateProductDto]
//
//  implicit def readProductDtoDecoder[F[_]: Concurrent]: EntityDecoder[F, ReadProductDto] =
//    org.http4s.circe.jsonOf[F, ReadProductDto]

  final case class CreateProductDto(
    name:        String,
    categoryId:  String,
    supplierId:  String,
    price:       Float,
    description: Option[String]
  )

  final case class UpdateProductDto(
    id:          String,
    name:        Option[String],
    categoryId:  Option[String],
    supplierId:  Option[String],
    price:       Option[Float],
    description: Option[String],
    status:      Option[ProductStatus]
  )

  final case class ReadProductDto(
    id:          String,
    name:        String,
    category:    CategoryDto,
    supplier:    SupplierDto,
    price:       Float,
    description: String,
    status:      ProductStatus
  )
}
