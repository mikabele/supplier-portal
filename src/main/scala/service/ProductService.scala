package service

import cats.effect.kernel.Sync
import dto.attachment.AttachmentCreateDto
import dto.criteria.CriteriaDto
import dto.product.{ProductCreateDto, ProductReadDto, ProductUpdateDto}
import repository.{OrderRepository, ProductRepository, SupplierRepository}
import service.impl.ProductServiceImpl
import util.ConvertToErrorsUtil.ErrorsOr

import java.util.UUID

trait ProductService[F[_]] {
  def addProduct(productDto:    ProductCreateDto): F[ErrorsOr[UUID]]
  def updateProduct(productDto: ProductUpdateDto): F[ErrorsOr[ProductUpdateDto]]
  def deleteProduct(id:         UUID):             F[ErrorsOr[Int]]
  def readProducts(userId:      UUID):             F[List[ProductReadDto]]
  def attach(attachmentDto:     AttachmentCreateDto): F[ErrorsOr[UUID]]
  def searchByCriteria(userId:  UUID, criteriaDto: CriteriaDto): F[ErrorsOr[List[ProductReadDto]]]
  def removeAttachment(id:      UUID):             F[ErrorsOr[Int]]
}

object ProductService {
  def of[F[_]: Sync](
    productRepository:  ProductRepository[F],
    supplierRepository: SupplierRepository[F],
    orderRepository:    OrderRepository[F]
  ): ProductService[F] = {
    new ProductServiceImpl[F](productRepository, supplierRepository, orderRepository)
  }
}
