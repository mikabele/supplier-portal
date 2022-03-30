package service.impl

import cats.Monad
import cats.data.{EitherT, NonEmptyChain, ValidatedNec}
import cats.syntax.all._
import domain.attachment._
import domain.criteria.Criteria
import domain.product._
import dto.attachment._
import dto.criteria.CriteriaDto
import dto.product._
import eu.timepit.refined.boolean.Not._
import eu.timepit.refined.numeric.Less._
import eu.timepit.refined.string.MatchesRegex._
import eu.timepit.refined.string.Url._
import eu.timepit.refined.string.Uuid._
import repository.ProductRepository
import service.ProductService
import service.error.validation.ValidationError
import service.error.validation.ValidationError._
import types._
import util.ModelMapper._
import util.RefinedValidator._

import java.util.UUID

class ProductServiceImpl[F[_]: Monad](
  productRep: ProductRepository[F]
) extends ProductService[F] {
  override def addProduct(productDto: CreateProductDto): F[Either[NonEmptyChain[ValidationError], CreateProductDto]] = {
    val res = for {
      product <- EitherT.fromEither(validateCreateProductDto(productDto).toEither)
      _       <- EitherT.liftF(productRep.addProduct(product))
    } yield createProductDomainToDto(product)

    res.value
  }

  override def updateProduct(
    productDto: UpdateProductDto
  ): F[Either[NonEmptyChain[ValidationError], UpdateProductDto]] = {
    val res = for {
      product <- EitherT.fromEither(validateUpdateProductDto(productDto).toEither)
      _       <- EitherT.liftF(productRep.updateProduct(product))
    } yield updateProductDomainToDto(product)

    res.value
  }

  override def deleteProduct(id: UUID): F[Unit] = {
    for {
      _ <- productRep.deleteProduct(id)
    } yield ()
  }

  override def readProducts(): F[List[ReadProductDto]] = {
    for {
      products <- productRep.viewProducts()
    } yield products.map(readProductDomainToDto)

  }

  override def attach(
    attachmentDto: CreateAttachmentDto
  ): F[Either[NonEmptyChain[ValidationError], CreateAttachmentDto]] = {
    val res = for {
      attachment <- EitherT.fromEither(validateAttachmentDto(attachmentDto).toEither)
      _          <- EitherT.liftF(productRep.attach(attachment))
    } yield attachmentDomainToDto(attachment)

    res.value
  }

  override def searchByCriteria(
    criteriaDto: CriteriaDto
  ): F[Either[NonEmptyChain[ValidationError], List[ReadProductDto]]] = {
    val res = for {
      criteria <- EitherT.fromEither(validateCriteriaDto(criteriaDto).toEither)
      products <- EitherT.liftF(productRep.searchByCriteria(criteria))
    } yield products.map(readProductDomainToDto)

    res.value
  }

  private def validateCreateProductDto(productDto: CreateProductDto): ValidatedNec[ValidationError, CreateProduct] = {
    val price: ValidatedNec[ValidationError, NonNegativeFloat] =
      refinedValidation(productDto.price, NegativeField("price"))
    (
      refinedValidation(productDto.name, EmptyField("name"))(matchesRegexValidate)
        .asInstanceOf[ValidatedNec[ValidationError, NonEmptyStr]],
      refinedValidation(productDto.categoryId, InvalidIdFormat("category_id"))(uuidValidate),
      refinedValidation(productDto.supplierId, InvalidIdFormat("supplier_id"))(uuidValidate),
      price,
      productDto.description.traverse(_.validNec)
    ).mapN(CreateProduct)
  }

  private def validateUpdateProductDto(dto: UpdateProductDto): ValidatedNec[ValidationError, UpdateProduct] = {
    val price: ValidatedNec[ValidationError, Option[NonNegativeFloat]] =
      dto.price.traverse(p => refinedValidation(p, NegativeField("price")))
    (
      refinedValidation(dto.id, InvalidIdFormat("id"))(uuidValidate),
      dto.name
        .traverse(n => refinedValidation(n, EmptyField("name"))(matchesRegexValidate))
        .asInstanceOf[ValidatedNec[ValidationError, Option[NonEmptyStr]]],
      dto.categoryId.traverse(i => refinedValidation(i, InvalidIdFormat("category_id"))(uuidValidate)),
      dto.supplierId.traverse(i => refinedValidation(i, InvalidIdFormat("supplier_id"))(uuidValidate)),
      price,
      dto.description.traverse(_.validNec),
      dto.status.traverse(_.validNec)
    ).mapN(UpdateProduct)
  }

  private def validateAttachmentDto(dto: CreateAttachmentDto): ValidatedNec[ValidationError, CreateAttachment] = {
    (
      refinedValidation(dto.attachment, InvalidUrlFormat("attachment"))(urlValidate),
      refinedValidation(dto.productId, InvalidIdFormat("product_id"))(uuidValidate)
    ).mapN(CreateAttachment)
  }

  private def validateCriteriaDto(dto: CriteriaDto): ValidatedNec[ValidationError, Criteria] = {
    (
      dto.name.traverse(_.validNec),
      dto.categoryId.traverse(i => refinedValidation(i, InvalidIdFormat("category_id"))(uuidValidate)),
      dto.description.traverse(_.validNec),
      dto.supplierId.traverse(i => refinedValidation(i, InvalidIdFormat("supplier_id"))(uuidValidate)),
      dto.publicationPeriod
        .traverse(p => refinedValidation(p, InvalidDateFormat("publication_period"))(matchesRegexValidate))
        .asInstanceOf[ValidatedNec[ValidationError, Option[DateStr]]]
    ).mapN(Criteria)
  }
}
