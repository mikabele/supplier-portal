package service

import cats.data.ValidatedNec
import dto.attachment.CreateAttachmentDto
import dto.criteria.CriteriaDto
import dto.product.{CreateProductDto, ReadProductDto, UpdateProductDto}
import repository.ProductRepository
import service.error.product.ProductError
import service.impl.ProductServiceImpl

trait ProductService[F[_]] {
  def addProduct(productDto:    CreateProductDto): F[ValidatedNec[ProductError, ReadProductDto]]
  def updateProduct(productDto: UpdateProductDto): F[ValidatedNec[ProductError, ReadProductDto]]
  def deleteProduct(id:         Int):              F[Either[ProductError, Unit]]
  def readProducts(): F[Either[ProductError, List[ReadProductDto]]]
  def attach(attachmentDto:         CreateAttachmentDto): F[ValidatedNec[ProductError, CreateAttachmentDto]]
  def searchByCriteria(criteriaDto: CriteriaDto):         F[ValidatedNec[ProductError, List[ReadProductDto]]]
}

object ProductService {
  def of[F[_]](productRepository: ProductRepository[F]): ProductService[F] = {
    new ProductServiceImpl[F](productRepository)
  }
}
