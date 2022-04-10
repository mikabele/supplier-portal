package service.impl

import cats.Monad
import cats.data.{Chain, EitherT, NonEmptyList}
import cats.syntax.all._
import domain.product.{ProductCreateDomain, ProductStatus}
import dto.attachment._
import dto.criteria.CriteriaDto
import dto.product._
import repository.{ProductRepository, SupplierRepository}
import service.ProductService
import service.error.attachment.AttachmentError.AttachmentNotFound
import service.error.general.GeneralError
import service.error.product.ProductError.ProductNotFound
import service.error.supplier.SupplierError.SupplierNotFound
import util.ConvertToErrorsUtil.ErrorsOr
import util.ModelMapper.DomainToDto._
import util.ModelMapper.DtoToDomain._
import util.ConvertToErrorsUtil._
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}

import java.util.UUID

class ProductServiceImpl[F[_]: Monad](
  productRep:         ProductRepository[F],
  supplierRepository: SupplierRepository[F]
) extends ProductService[F] {
  override def addProduct(productDto: ProductCreateDto): F[ErrorsOr[UUID]] = {
    val res = for {
      product <- validateCreateProductDto(productDto).toErrorsOr(fromValidatedNec)
      _ <- EitherT.fromOptionF(
        supplierRepository.getById(product.supplierId),
        Chain[GeneralError](SupplierNotFound(product.supplierId.value))
      )
      id <- productRep.addProduct(product).toErrorsOr
    } yield id

    res.value
  }

  override def updateProduct(
    productDto: ProductUpdateDto
  ): F[ErrorsOr[ProductUpdateDto]] = {
    val res = for {
      domain <- validateUpdateProductDto(productDto).toErrorsOr(fromValidatedNec)
      _ <- EitherT.fromOptionF(
        supplierRepository.getById(domain.supplierId),
        Chain[GeneralError](SupplierNotFound(domain.supplierId.value))
      )
      count <- productRep.updateProduct(domain).toErrorsOr
      _     <- EitherT.cond(count > 0, count, Chain[GeneralError](ProductNotFound(domain.id.value)))
    } yield updateProductDomainToDto(domain)

    res.value
  }

  override def deleteProduct(id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      count  <- productRep.deleteProduct(id).toErrorsOr
      result <- EitherT.cond(count > 0, count, Chain[GeneralError](ProductNotFound(id.toString)))
    } yield result

    res.value
  }

  override def readProducts(userId: UUID): F[List[ProductReadDto]] = {
    for {
      products <- productRep.viewProducts(userId, NonEmptyList.of(ProductStatus.Available, ProductStatus.NotAvailable))
    } yield products.map(readProductDomainToDto)

  }

  override def attach(
    attachmentDto: AttachmentCreateDto
  ): F[ErrorsOr[UUID]] = {
    val res = for {
      attachment <- validateAttachmentDto(attachmentDto).toErrorsOr(fromValidatedNec)
      products   <- productRep.getByIds(NonEmptyList.of(attachment.productId)).toErrorsOr
      _ <- EitherT.cond(
        products.nonEmpty,
        products,
        Chain[GeneralError](ProductNotFound(attachment.productId.value))
      )
      id <- productRep.attach(attachment).toErrorsOr
    } yield id

    res.value
  }

  override def searchByCriteria(
    userId:      UUID,
    criteriaDto: CriteriaDto
  ): F[ErrorsOr[List[ProductReadDto]]] = {
    val res = for {
      criteria <- validateCriteriaDto(criteriaDto).toErrorsOr(fromValidatedNec)
      products <- productRep.searchByCriteria(userId, criteria).toErrorsOr
    } yield products.map(readProductDomainToDto)

    res.value
  }

  override def removeAttachment(id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      count  <- productRep.removeAttachment(id).toErrorsOr
      result <- EitherT.cond(count > 0, count, Chain[GeneralError](AttachmentNotFound(id.toString)))
    } yield result

    res.value
  }
}
