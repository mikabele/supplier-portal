package dto

import domain.category.CategoryDomain
import domain.product.ProductStatus
import dto.attachment.AttachmentReadDto
import dto.supplier.SupplierDto

object product {

  final case class ProductCreateDto(
    name:        String,
    categoryId:  Int,
    supplierId:  Int,
    price:       Float,
    description: Option[String]
  )

  final case class ProductUpdateDto(
    id:          String,
    name:        String,
    categoryId:  Int,
    supplierId:  Int,
    price:       Float,
    description: String,
    status:      ProductStatus
  )

  final case class ProductReadDto(
    id:              String,
    name:            String,
    category:        CategoryDomain,
    supplier:        SupplierDto,
    price:           Float,
    description:     String,
    status:          ProductStatus,
    publicationDate: String,
    attachments:     List[AttachmentReadDto]
  )
}
