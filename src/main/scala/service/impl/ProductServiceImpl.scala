package service.impl

import cats.Monad
import cats.data.{Chain, EitherT}
import cats.syntax.all._
import eu.timepit.refined.string.Uuid._
import java.util.UUID

import domain.criteria.Criteria
import dto.attachment._
import dto.criteria.CriteriaDto
import dto.product._
import repository.ProductRepository
import service.ProductService
import service.error.general.{ErrorsOr, GeneralError}
import service.error.product.ProductError.ProductNotFound
import util.ModelMapper._
import util.RefinedValidator._

class ProductServiceImpl[F[_]: Monad](
  productRep: ProductRepository[F]
) extends ProductService[F] {
  override def addProduct(productDto: CreateProductDto): F[ErrorsOr[UUID]] = {
    val res = for {
      product <- EitherT.fromEither(validateCreateProductDto(productDto).toEither.leftMap(_.toChain))
      id      <- EitherT.liftF(productRep.addProduct(product)).leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield id

    res.value
  }

  override def updateProduct(
    productDto: UpdateProductDto
  ): F[ErrorsOr[UpdateProductDto]] = {
    val res = for {
      id      <- EitherT.fromEither(refinedValidation(productDto.id)(uuidValidate).toEither).leftMap(_.toChain)
      criteria = Criteria(id.pure[Option])
      readDomains <- EitherT
        .liftF(productRep.searchByCriteria(criteria))
        .leftMap((_: Nothing) => Chain.empty[GeneralError])
      readDomain <- EitherT.fromOption(
        readDomains.headOption,
        Chain[GeneralError](ProductNotFound(UUID.fromString(id.value)))
      )
      updateDomain = readToUpdateProduct(readDomain)
      _           <- EitherT.liftF(productRep.updateProduct(updateDomain)).leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield updateProductDomainToDto(updateDomain)

    res.value
  }

  override def deleteProduct(id: UUID): F[ErrorsOr[Int]] = {
    val res = for {
      count  <- EitherT.liftF(productRep.deleteProduct(id)).leftMap((_: Nothing) => Chain.empty[GeneralError])
      result <- EitherT.fromEither(Either.cond(count > 0, count, Chain[GeneralError](ProductNotFound(id))))
    } yield result

    res.value
  }

  override def readProducts(): F[List[ReadProductDto]] = {
    for {
      products <- productRep.viewProducts()
    } yield products.map(readProductDomainToDto)

  }

  override def attach(
    attachmentDto: CreateAttachmentDto
  ): F[ErrorsOr[UUID]] = {
    val res = for {
      attachment <- EitherT.fromEither(validateAttachmentDto(attachmentDto).toEither).leftMap(_.toChain)
      id         <- EitherT.liftF(productRep.attach(attachment)).leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield id

    res.value
  }

  override def searchByCriteria(
    criteriaDto: CriteriaDto
  ): F[ErrorsOr[List[ReadProductDto]]] = {
    val res = for {
      criteria <- EitherT.fromEither(validateCriteriaDto(criteriaDto).toEither.leftMap(_.toChain))
      products <- EitherT
        .liftF(productRep.searchByCriteria(criteria))
        .leftMap((_: Nothing) => Chain.empty[GeneralError])
    } yield products.map(readProductDomainToDto)

    res.value
  }

}
