package service

import cats.data.NonEmptyChain
import cats.effect.kernel.Sync
import dto.attachment.CreateAttachmentDto
import dto.criteria.CriteriaDto
import dto.product.{CreateProductDto, ReadProductDto, UpdateProductDto}
import repository.ProductRepository
import service.error.validation.ValidationError
import service.impl.ProductServiceImpl

import java.util.UUID

trait ProductService[F[_]] {
  def addProduct(productDto:    CreateProductDto): F[Either[NonEmptyChain[ValidationError], CreateProductDto]]
  def updateProduct(productDto: UpdateProductDto): F[Either[NonEmptyChain[ValidationError], UpdateProductDto]]
  def deleteProduct(id:         UUID):             F[Unit]
  def readProducts(): F[List[ReadProductDto]]
  def attach(attachmentDto:         CreateAttachmentDto): F[Either[NonEmptyChain[ValidationError], CreateAttachmentDto]]
  def searchByCriteria(criteriaDto: CriteriaDto):         F[Either[NonEmptyChain[ValidationError], List[ReadProductDto]]]
}

object ProductService {
  def of[F[_]: Sync](productRepository: ProductRepository[F]): ProductService[F] = {
    new ProductServiceImpl[F](productRepository)
  }
}
