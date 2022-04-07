package util

import cats.data.ValidatedNec
import cats.implicits._
import domain.attachment._
import domain.criteria.Criteria
import domain.delivery.{CreateDelivery, ReadDelivery}
import domain.order.{CreateOrder, CreateOrderItem, DbReadOrder, ReadOrder, ReadOrderItem, UpdateOrder}
import domain.product._
import domain.subscription.{CategorySubscription, SupplierSubscription}
import domain.supplier.Supplier
import domain.user.ReadAuthorizedUser
import dto.attachment._
import dto.criteria.CriteriaDto
import dto.delivery.{CreateDeliveryDto, ReadDeliveryDto}
import dto.order.{CreateOrderDto, OrderItemDto, ReadOrderDto, UpdateOrderDto}
import dto.product._
import dto.subscription.{CategorySubscriptionDto, SupplierSubscriptionDto}
import dto.supplier.SupplierDto
import dto.user.ReadAuthorizedUserDto
import service.error.general.GeneralError
import types._
import util.RefinedValidator.refinedValidation

object ModelMapper {
  // TODO - find out how to make isomorphism between Dto and Domain (refined supported)

  def createProductDomainToDto(product: CreateProduct): CreateProductDto = {
    CreateProductDto(
      product.name.value,
      product.category,
      product.supplierId.value,
      product.price.value,
      product.description
    )
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
    val attachment: ValidatedNec[GeneralError, UrlStr]  = refinedValidation(dto.attachment)
    val productId:  ValidatedNec[GeneralError, UuidStr] = refinedValidation(dto.productId)
    (
      attachment,
      productId
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

  def validateOrderItemDto(dto: OrderItemDto): ValidatedNec[GeneralError, CreateOrderItem] = {
    val id:    ValidatedNec[GeneralError, UuidStr]     = refinedValidation(dto.productId)
    val count: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(dto.count)
    (
      id,
      count
    ).mapN(CreateOrderItem)
  }

  def validateCreateOrderDto(dto: CreateOrderDto): ValidatedNec[GeneralError, CreateOrder] = {
    val id:         ValidatedNec[GeneralError, UuidStr]               = refinedValidation(dto.userId)
    val orderItems: ValidatedNec[GeneralError, List[CreateOrderItem]] = dto.orderItems.traverse(validateOrderItemDto)
    val address:    ValidatedNec[GeneralError, NonEmptyStr]           = refinedValidation(dto.address)
    (
      id,
      orderItems,
      0f.validNec,
      address
    ).mapN(CreateOrder)
  }

  def orderItemDomainToDto(domain: ReadOrderItem): OrderItemDto = {
    OrderItemDto(domain.productId.value, domain.count.value)
  }

  def readOrderDomainToDto(domain: ReadOrder): ReadOrderDto = {
    val orderItems = domain.orderItems.map(orderItemDomainToDto)
    ReadOrderDto(
      domain.id.value,
      domain.userId.value,
      orderItems,
      domain.orderStatus,
      domain.orderedStartDate.value,
      domain.total.value,
      domain.address.value
    )
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

  def readAuthorizedUserDomainToDto(user: ReadAuthorizedUser): ReadAuthorizedUserDto = {
    ReadAuthorizedUserDto(
      user.id.value,
      user.name.value,
      user.surname.value,
      user.role,
      user.phone.value,
      user.email.value
    )
  }

  def readDeliveryDomainToDto(domain: ReadDelivery): ReadDeliveryDto = {
    ReadDeliveryDto(
      domain.id.value,
      domain.orderId.value,
      readAuthorizedUserDomainToDto(domain.courier),
      domain.deliveryStartDate.value,
      domain.deliveryFinishDate.map(_.value)
    )
  }

  def validateCreateDeliveryDto(dto: CreateDeliveryDto): ValidatedNec[GeneralError, CreateDelivery] = {
    val courierId: ValidatedNec[GeneralError, UuidStr] = refinedValidation(dto.courierId)
    val orderId:   ValidatedNec[GeneralError, UuidStr] = refinedValidation(dto.orderId)
    (
      courierId,
      orderId
    ).mapN(CreateDelivery)
  }

  object DbModelMapper {

    def joinProductsWithAttachments(
      products:    List[DbReadProduct],
      attachments: List[ReadAttachment]
    ): List[ReadProduct] = {
      products.map(product =>
        ReadProduct(
          product.id,
          product.name,
          product.category,
          product.supplier,
          product.price,
          product.description,
          product.status,
          product.publicationPeriod,
          attachments.filter(_.productId == product.id)
        )
      )
    }

    def joinOrdersWithProducts(orders: List[DbReadOrder], orderProducts: List[ReadOrderItem]): List[ReadOrder] = {
      orders.map(order =>
        ReadOrder(
          order.id,
          order.userId,
          orderProducts.filter(_.orderId == order.id),
          order.orderStatus,
          order.orderedStartDate,
          order.total,
          order.address
        )
      )
    }
  }
}
