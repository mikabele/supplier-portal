package util

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
import domain.order.{CreateOrder, OrderItem, ReadOrder, UpdateOrder}
import domain.product._
import domain.subscription.{CategorySubscription, SupplierSubscription}
import domain.supplier.Supplier
import types._
import util.RefinedValidator.refinedValidation
import service.error.general.GeneralError
import dto.attachment._
import dto.criteria.CriteriaDto
import dto.order.{CreateOrderDto, OrderItemDto, ReadOrderDto, UpdateOrderDto}
import dto.product._
import dto.subscription.{CategorySubscriptionDto, SupplierSubscriptionDto}
import dto.supplier.SupplierDto

object ModelMapper {
  // TODO - find out how to make isomorphism between Dto and Domain (refined supported)

  def createProductDomainToDto(product: CreateProduct): CreateProductDto = {
//    val domainFields = GenIso.fields[CreateProduct]
//    val dtoFields    = GenIso.fields[CreateProductDto].reverse
//    val domainToDto  = domainFields.composeIso(dtoFields)
    CreateProductDto(
      product.name.value,
      product.category,
      product.supplierId.value,
      product.price.value,
      product.description
    )
    //domainToDto.get(product)
  }

  def updateProductDomainToDto(product: UpdateProduct): UpdateProductDto = {
    UpdateProductDto(
      product.id.value,
      product.name.value,
      product.category,
      product.supplierId.value,
      product.price.value,
      product.description,
      product.status
    )
  }

  def readAttachmentDomainToDto(domain: ReadAttachment): ReadAttachmentDto = {
    ReadAttachmentDto(domain.id.value, domain.attachment.value)
  }

  def readProductDomainToDto(product: ReadProduct): ReadProductDto = {
    ReadProductDto(
      product.id.value,
      product.name.value,
      product.category,
      supplierDomainToDto(product.supplier),
      product.price.value,
      product.description,
      product.status,
      product.publicationPeriod.value,
      product.attachments.map(readAttachmentDomainToDto)
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

  def validateCreateProductDto(productDto: CreateProductDto): ValidatedNec[GeneralError, CreateProduct] = {
    val price: ValidatedNec[GeneralError, NonNegativeFloat] =
      refinedValidation(productDto.price)
    val name:     ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation(productDto.name)
    val supplier: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(productDto.supplierId)
    (
      name,
      productDto.category.validNec,
      supplier,
      price,
      productDto.description.traverse(_.validNec)
    ).mapN(CreateProduct)
  }

  def validateUpdateProductDto(dto: UpdateProductDto): ValidatedNec[GeneralError, UpdateProduct] = {
    val id:         ValidatedNec[GeneralError, UuidStr]          = refinedValidation(dto.id)
    val name:       ValidatedNec[GeneralError, NonEmptyStr]      = refinedValidation(dto.name)
    val supplierId: ValidatedNec[GeneralError, PositiveInt]      = refinedValidation(dto.supplierId)
    val price:      ValidatedNec[GeneralError, NonNegativeFloat] = refinedValidation(dto.price)
    (
      id,
      name,
      dto.category.validNec,
      supplierId,
      price,
      dto.description.validNec,
      dto.status.validNec
    ).mapN(UpdateProduct)
  }

  def validateAttachmentDto(dto: CreateAttachmentDto): ValidatedNec[GeneralError, CreateAttachment] = {
    (
      refinedValidation(dto.attachment)(urlValidate),
      refinedValidation(dto.productId)(uuidValidate)
    ).mapN(CreateAttachment)
  }

  def validateCriteriaDto(dto: CriteriaDto): ValidatedNec[GeneralError, Criteria] = {
    val startDate: ValidatedNec[GeneralError, Option[DateStr]] = dto.startDate.traverse(p => refinedValidation(p))
    val endDate:   ValidatedNec[GeneralError, Option[DateStr]] = dto.endDate.traverse(p => refinedValidation(p))
    (
      dto.name.traverse(_.validNec),
      dto.categoryName.traverse(_.validNec),
      dto.description.traverse(_.validNec),
      dto.supplierName.traverse(_.validNec),
      dto.status.validNec,
      startDate,
      endDate
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
    val userId: ValidatedNec[GeneralError, UuidStr] = refinedValidation(dto.userId)
    (
      userId,
      dto.category.validNec
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

  def validateOrderItemDto(dto: OrderItemDto): ValidatedNec[GeneralError, OrderItem] = {
    val id:    ValidatedNec[GeneralError, UuidStr]     = refinedValidation(dto.productId)
    val count: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(dto.count)
    (
      id,
      count
    ).mapN(OrderItem)
  }

  def validateCreateOrderDto(dto: CreateOrderDto): ValidatedNec[GeneralError, CreateOrder] = {
    val id:         ValidatedNec[GeneralError, UuidStr]         = refinedValidation(dto.userId)
    val orderItems: ValidatedNec[GeneralError, List[OrderItem]] = dto.orderItems.traverse(validateOrderItemDto)
    (
      id,
      orderItems,
      0f.validNec
    ).mapN(CreateOrder)
  }

  def orderItemDomainToDto(domain: OrderItem): OrderItemDto = {
    OrderItemDto(domain.productId.value, domain.count.value)
  }

  def readOrderDomainToDto(domain: ReadOrder): ReadOrderDto = {
    val orderItems = domain.orderItems.map(orderItemDomainToDto)
    ReadOrderDto(domain.id.value, orderItems, domain.orderStatus, domain.orderedStartDate.value, domain.total.value)
  }

  def updateOrderDomainToDto(domain: UpdateOrder): UpdateOrderDto = {
    UpdateOrderDto(domain.id.value, domain.orderStatus)
  }

  def validateUpdateOrderDto(dto: UpdateOrderDto): ValidatedNec[GeneralError, UpdateOrder] = {
    val id: ValidatedNec[GeneralError, UuidStr] = refinedValidation(dto.id)
    (
      id,
      dto.orderStatus.validNec
    ).mapN(UpdateOrder)
  }

  def validateReadAttachmentDto(dto: ReadAttachmentDto): ValidatedNec[GeneralError, ReadAttachment] = {
    val id:  ValidatedNec[GeneralError, UuidStr] = refinedValidation(dto.id)
    val url: ValidatedNec[GeneralError, UrlStr]  = refinedValidation(dto.attachment)
    (
      id,
      url
    ).mapN(ReadAttachment)
  }
}
