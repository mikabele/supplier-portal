package service

import cats.effect.kernel.Sync
import dto.attachment.CreateAttachmentDto
import dto.criteria.CriteriaDto
import dto.product.{CreateProductDto, ReadProductDto, UpdateProductDto}
import repository.{ProductRepository, SupplierRepository}
import service.error.general.ErrorsOr
import service.impl.ProductServiceImpl

import java.util.UUID

trait ProductService[F[_]] {
  def addProduct(productDto:    CreateProductDto): F[ErrorsOr[UUID]]
  def updateProduct(productDto: UpdateProductDto): F[ErrorsOr[UpdateProductDto]]
  def deleteProduct(id:         UUID):             F[ErrorsOr[Int]]
  def readProducts(): F[List[ReadProductDto]]
  def attach(attachmentDto:         CreateAttachmentDto): F[ErrorsOr[UUID]]
  def searchByCriteria(criteriaDto: CriteriaDto):         F[ErrorsOr[List[ReadProductDto]]]
  def removeAttachment(id:          UUID):                F[ErrorsOr[Int]]
}

object ProductService {
  def of[F[_]: Sync](
    productRepository:  ProductRepository[F],
    supplierRepository: SupplierRepository[F]
  ): ProductService[F] = {
    new ProductServiceImpl[F](productRepository, supplierRepository)
  }
}