package dto

import domain.category.Category
import domain.product.ProductStatus
import dto.attachment.AttachmentReadDto
import dto.supplier.SupplierDto

object product {

  final case class ProductCreateDto(
    name:        String,
    category:    Category,
    supplierId:  Int,
    price:       Float,
    description: Option[String]
  )

  final case class ProductUpdateDto(
    id:          String,
    name:        String,
    category:    Category,
    supplierId:  Int,
    price:       Float,
    description: String,
    status:      ProductStatus
  )

  final case class ProductReadDto(
    id:              String,
    name:            String,
    category:        Category,
    supplier:        SupplierDto,
    price:           Float,
    description:     String,
    status:          ProductStatus,
    publicationDate: String,
    attachments:     List[AttachmentReadDto]
  )
}
