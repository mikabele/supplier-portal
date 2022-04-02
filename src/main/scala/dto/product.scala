package dto

import domain.category.Category
import domain.product.ProductStatus
import dto.supplier.SupplierDto

object product {

  final case class CreateProductDto(
    name:        String,
    categoryId:  Int,
    supplierId:  Int,
    price:       Float,
    description: Option[String]
  )

  final case class UpdateProductDto(
    id:          String,
    name:        Option[String],
    categoryId:  Option[Int],
    supplierId:  Option[Int],
    price:       Option[Float],
    description: Option[String],
    status:      Option[ProductStatus]
  )

  final case class ReadProductDto(
    id:          String,
    name:        String,
    category:    Category,
    supplier:    SupplierDto,
    price:       Float,
    description: String,
    status:      ProductStatus
  )
}
