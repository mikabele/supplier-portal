package dto

import domain.category.Category
import domain.product.ProductStatus
import dto.attachment.ReadAttachmentDto
import dto.supplier.SupplierDto

object product {

  final case class CreateProductDto(
    name:        String,
    category:    Category,
    supplierId:  Int,
    price:       Float,
    description: Option[String]
  )

  final case class UpdateProductDto(
    id:          String,
    name:        String,
    category:    Category,
    supplierId:  Int,
    price:       Float,
    description: String,
    status:      ProductStatus
  )

  final case class ReadProductDto(
    id:                String,
    name:              String,
    category:          Category,
    supplier:          SupplierDto,
    price:             Float,
    description:       String,
    status:            ProductStatus,
    publicationPeriod: String,
    attachments:       List[ReadAttachmentDto]
  )
}
