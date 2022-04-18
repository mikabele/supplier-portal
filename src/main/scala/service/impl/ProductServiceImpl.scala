package service.impl

import cats.Monad
import cats.data.{Chain, EitherT, NonEmptyList}
import cats.syntax.all._
import domain.product.ProductStatus
import domain.user.AuthorizedUserDomain
import dto.attachment._
import dto.criteria.CriteriaDto
import dto.product._
import logger.LogHandler
import repository.{OrderRepository, ProductRepository, SupplierRepository}
import service.ProductService
import service.error.attachment.AttachmentError.{AttachmentExists, AttachmentNotFound}
import service.error.general.GeneralError
import service.error.product.ProductError.{DeclineDeleteProduct, ProductExists, ProductNotFound}
import service.error.supplier.SupplierError.SupplierNotFound
import util.ConvertToErrorsUtil._
import util.ConvertToErrorsUtil.instances.{fromF, fromValidatedNec}
import util.ModelMapper.DomainToDto._
import util.ModelMapper.DtoToDomain._

import java.util.UUID

class ProductServiceImpl[F[_]: Monad](
  productRep:         ProductRepository[F],
  supplierRepository: SupplierRepository[F],
  orderRepository:    OrderRepository[F],
  logHandler:         LogHandler[F]
) extends ProductService[F] {
  override def addProduct(productDto: ProductCreateDto): F[ErrorsOr[UUID]] = {
    val res = for {
      _       <- logHandler.debug(s"Start validation : ProductCreateDto").toErrorsOr
      product <- validateCreateProductDto(productDto).toErrorsOr(fromValidatedNec)
      _       <- logHandler.debug(s"Validation finished : ProductCreateDto").toErrorsOr
      _ <- EitherT.fromOptionF(
        supplierRepository.getById(product.supplierId),
        Chain[GeneralError](SupplierNotFound(product.supplierId.value))
      )
      _     <- logHandler.debug(s"Supplier found ").toErrorsOr
      check <- productRep.checkUniqueProduct(product.name.value, product.supplierId.value).toErrorsOr
      _     <- logHandler.debug(s"Given product has unique name and supplier pair").toErrorsOr
      _ <- EitherT.cond(
        check.isEmpty,
        (),
        Chain[GeneralError](ProductExists(product.name.value, product.supplierId.value))
      )
      id <- productRep.addProduct(product).toErrorsOr
      _  <- logHandler.debug(s"Product added").toErrorsOr
    } yield id

    res.value
  }

  override def updateProduct(
    productDto: ProductUpdateDto
  ): F[ErrorsOr[ProductUpdateDto]] = {
    val res = for {
      _      <- logHandler.debug(s"Start validation : ProductUpdateDto").toErrorsOr
      domain <- validateUpdateProductDto(productDto).toErrorsOr(fromValidatedNec)
      _      <- logHandler.debug(s"Validation finished : ProductUpdateDto").toErrorsOr
      _ <- EitherT.fromOptionF(
        supplierRepository.getById(domain.supplierId),
        Chain[GeneralError](SupplierNotFound(domain.supplierId.value))
      )
      _     <- logHandler.debug(s"Supplier found ").toErrorsOr
      check <- productRep.checkUniqueProduct(domain.name.value, domain.supplierId.value).toErrorsOr
      _ <- EitherT.cond(
        check.isEmpty,
        (),
        Chain[GeneralError](ProductExists(domain.name.value, domain.supplierId.value))
      )
      _     <- logHandler.debug(s"Product found ").toErrorsOr
      count <- productRep.updateProduct(domain).toErrorsOr
      _     <- EitherT.cond(count > 0, count, Chain[GeneralError](ProductNotFound(domain.id.value)))
      _     <- logHandler.debug(s"Product updated").toErrorsOr
    } yield updateProductDomainToDto(domain)

    res.value
  }

  override def deleteProduct(id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      orderCount <- orderRepository.checkActiveOrderWithProduct(id).toErrorsOr
      _          <- EitherT.cond(orderCount == 0, (), Chain[GeneralError](DeclineDeleteProduct(orderCount)))
      _          <- logHandler.debug(s"There are some active orders with this product, you can't delete product").toErrorsOr
      count      <- productRep.deleteProduct(id).toErrorsOr
      result     <- EitherT.cond(count > 0, count, Chain[GeneralError](ProductNotFound(id.toString)))
      _          <- logHandler.debug(s"Product deleted").toErrorsOr
    } yield result

    res.value
  }

  override def readProducts(user: AuthorizedUserDomain): F[List[ProductReadDto]] = {
    for {
      products <- productRep.viewProducts(user, NonEmptyList.of(ProductStatus.Available, ProductStatus.NotAvailable))
      _        <- logHandler.debug(s"Found some products : $products")
    } yield products.map(readProductDomainToDto)

  }

  override def attach(
    attachmentDto: AttachmentCreateDto
  ): F[ErrorsOr[UUID]] = {
    val res = for {
      _          <- logHandler.debug(s"Start validation : AttachmentCreateDto").toErrorsOr
      attachment <- validateAttachmentDto(attachmentDto).toErrorsOr(fromValidatedNec)
      _          <- logHandler.debug(s"Validation finished : AttachmentCreateDto").toErrorsOr
      products   <- productRep.getByIds(NonEmptyList.of(attachment.productId)).toErrorsOr
      _          <- logHandler.debug(s"Products in DB : $products").toErrorsOr
      product <- EitherT.fromOption(
        products.headOption,
        Chain[GeneralError](ProductNotFound(attachment.productId.value))
      )
      _ <- logHandler.debug(s"Product found").toErrorsOr
      _ <- EitherT.cond(
        !product.attachments.map(_.attachment).contains(attachment.attachment),
        (),
        Chain[GeneralError](AttachmentExists)
      )
      id <- productRep.attach(attachment).toErrorsOr
      _  <- logHandler.debug(s"Attachment created").toErrorsOr
    } yield id

    res.value
  }

  override def searchByCriteria(
    user:        AuthorizedUserDomain,
    criteriaDto: CriteriaDto
  ): F[ErrorsOr[List[ProductReadDto]]] = {
    val res = for {
      _        <- logHandler.debug(s"Start validation : CriteriaDto").toErrorsOr
      criteria <- validateCriteriaDto(criteriaDto).toErrorsOr(fromValidatedNec)
      _        <- logHandler.debug(s"Validation finished : CriteriaDto").toErrorsOr
      products <- productRep.searchByCriteria(user, criteria).toErrorsOr
    } yield products.map(readProductDomainToDto)

    res.value
  }

  override def removeAttachment(id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      count  <- productRep.removeAttachment(id).toErrorsOr
      result <- EitherT.cond(count > 0, count, Chain[GeneralError](AttachmentNotFound(id.toString)))
      _      <- logHandler.debug(s"Attachment removed").toErrorsOr
    } yield result

    res.value
  }
}
