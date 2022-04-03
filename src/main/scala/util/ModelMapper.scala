package util

import cats.data.Validated.Valid
import cats.data.ValidatedNec
import cats.implicits._
import eu.timepit.refined.string.MatchesRegex.matchesRegexValidate
import eu.timepit.refined.string.Url.urlValidate
import eu.timepit.refined.string.Uuid.uuidValidate
import monocle._
import monocle.refined._
import monocle.macros._

import domain.attachment._
import domain.criteria.Criteria
import domain.product._
import domain.subscription.{CategorySubscription, SupplierSubscription}
import domain.supplier.Supplier
import types._
import util.RefinedValidator.refinedValidation
import service.error.general.GeneralError
import dto.attachment._
import dto.criteria.CriteriaDto
import dto.product._
import dto.subscription.{CategorySubscriptionDto, SupplierSubscriptionDto}
import dto.supplier.SupplierDto

object ModelMapper {
  // TODO - read about scalaland lib
  // TODO - find out how to deal with Refined And ScalaLAnd at the same time

  def createProductDomainToDto(product: CreateProduct): CreateProductDto = {
//    val domainFields = GenIso.fields[CreateProduct]
//    val dtoFields    = GenIso.fields[CreateProductDto].reverse
//    val domainToDto  = domainFields.composeIso(dtoFields)
    CreateProductDto(
      product.name.value,
      product.categoryId.value,
      product.supplierId.value,
      product.price.value,
      product.description
    )
    //domainToDto.get(product)
  }

  def updateProductDomainToDto(product: UpdateProduct): UpdateProductDto = {
    UpdateProductDto(
      product.id.value,
      product.name.value.pure[Option],
      product.categoryId.value.pure[Option],
      product.supplierId.value.pure[Option],
      product.price.value.pure[Option],
      product.description.pure[Option],
      product.status.pure[Option]
    )
  }

  def readProductDomainToDto(product: ReadProduct): ReadProductDto = {
    ReadProductDto(
      product.id.value,
      product.name.value,
      product.category,
      supplierDomainToDto(product.supplier),
      product.price.value,
      product.description,
      product.status
    )
  }

  def attachmentDomainToDto(attachment: CreateAttachment): CreateAttachmentDto = {
    CreateAttachmentDto(
      attachment.attachment.value,
      attachment.productId.value
    )
  }

  def supplierDomainToDto(supplier: Supplier): SupplierDto = {
    SupplierDto(
      supplier.id.value,
      supplier.name.value,
      supplier.address.value
    )
  }

  def readToUpdateProduct(readDomain: ReadProduct): UpdateProduct = {
    val categoryId = (refinedValidation(readDomain.category.id): ValidatedNec[GeneralError, PositiveInt]) match {
      case Valid(a) => a
    }
    UpdateProduct(
      readDomain.id,
      readDomain.name,
      categoryId,
      readDomain.supplier.id,
      readDomain.price,
      readDomain.description,
      readDomain.status
    )
  }

  def validateCreateProductDto(productDto: CreateProductDto): ValidatedNec[GeneralError, CreateProduct] = {
    val price: ValidatedNec[GeneralError, NonNegativeFloat] =
      refinedValidation(productDto.price)
    val name:     ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation(productDto.name)
    val category: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(productDto.categoryId)
    val supplier: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(productDto.supplierId)
    (
      name,
      category,
      supplier,
      price,
      productDto.description.traverse(_.validNec)
    ).mapN(CreateProduct)
  }

  def mergeUpdateProductDtoWithDomain(
    dto:    UpdateProductDto,
    domain: UpdateProduct
  ): ValidatedNec[GeneralError, UpdateProduct] = {
    val price:    Option[ValidatedNec[GeneralError, NonNegativeFloat]] = dto.price.map(i => refinedValidation(i))
    val name:     Option[ValidatedNec[GeneralError, NonEmptyStr]]      = dto.name.map(i => refinedValidation(i))
    val category: Option[ValidatedNec[GeneralError, PositiveInt]]      = dto.categoryId.map(i => refinedValidation(i))
    val supplier: Option[ValidatedNec[GeneralError, PositiveInt]]      = dto.supplierId.map(i => refinedValidation(i))
    (
      domain.id.validNec,
      name.getOrElse(domain.name.validNec),
      category.getOrElse(domain.categoryId.validNec),
      supplier.getOrElse(domain.supplierId.validNec),
      price.getOrElse(domain.price.validNec),
      dto.description.getOrElse(domain.description).validNec,
      dto.status.getOrElse(domain.status).validNec
    ).mapN(UpdateProduct)
  }

  def validateAttachmentDto(dto: CreateAttachmentDto): ValidatedNec[GeneralError, CreateAttachment] = {
    (
      refinedValidation(dto.attachment)(urlValidate),
      refinedValidation(dto.productId)(uuidValidate)
    ).mapN(CreateAttachment)
  }

  def validateCriteriaDto(dto: CriteriaDto): ValidatedNec[GeneralError, Criteria] = {
    val publicationPeriod: ValidatedNec[GeneralError, Option[DateStr]] = dto.publicationPeriod
      .traverse(p => refinedValidation(p))
    val category: ValidatedNec[GeneralError, Option[PositiveInt]] =
      dto.categoryId.traverse(i => refinedValidation(i))
    val supplier: ValidatedNec[GeneralError, Option[PositiveInt]] =
      dto.supplierId.traverse(i => refinedValidation(i))
    val id: ValidatedNec[GeneralError, Option[UuidStr]] = dto.id.traverse(i => refinedValidation(i))
    (
      id,
      dto.name.traverse(_.validNec),
      category,
      dto.description.traverse(_.validNec),
      supplier,
      publicationPeriod
    ).mapN(Criteria)
  }

  def validateSupplierDto(dto: SupplierDto): ValidatedNec[GeneralError, Supplier] = {
    val name:    ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation(dto.name)
    val address: ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation(dto.address)
    val id:      ValidatedNec[GeneralError, PositiveInt] = refinedValidation(dto.id)
    (
      id,
      name,
      address
    ).mapN(Supplier)
  }

  def validateCategorySubscriptionDto(
    dto: CategorySubscriptionDto
  ): ValidatedNec[GeneralError, CategorySubscription] = {
    val userId:     ValidatedNec[GeneralError, UuidStr]     = refinedValidation(dto.userId)
    val categoryId: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(dto.categoryId)
    (
      userId,
      categoryId
    ).mapN(CategorySubscription)
  }

  def validateSupplierSubscriptionDto(
    dto: SupplierSubscriptionDto
  ): ValidatedNec[GeneralError, SupplierSubscription] = {
    val userId:     ValidatedNec[GeneralError, UuidStr]     = refinedValidation(dto.userId)
    val supplierId: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(dto.supplierId)
    (
      userId,
      supplierId
    ).mapN(SupplierSubscription)
  }
}
