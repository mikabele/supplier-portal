package util

import cats.data.{NonEmptyList, ValidatedNec}
import cats.syntax.all._
import domain.attachment._
import domain.category.CategoryDomain
import domain.criteria._
import domain.delivery._
import domain.group._
import domain.order._
import domain.product._
import domain.subscription._
import domain.supplier._
import domain.user._
import dto.attachment._
import dto.category.CategoryDto
import dto.criteria._
import dto.delivery._
import dto.group._
import dto.order._
import dto.product._
import dto.subscription._
import dto.supplier._
import dto.user._
import error.general.GeneralError
import error.group.ProductGroupError.{DuplicatedProductInGroup, DuplicatedUserInGroup, EmptyGroup}
import types._
import util.RefinedValidator.refinedValidation

object ModelMapper {

  object DomainToDto {

    def readProductGroupDomainToDto(domain: GroupReadDomain): GroupReadDto = {
      GroupReadDto(domain.id.value, domain.name.value, domain.userIds.map(_.value), domain.productIds.map(_.value))
    }

    def readAuthorizedUserDomainToDto(user: AuthorizedUserDomain): AuthorizedUserDto = {
      AuthorizedUserDto(
        user.id.value,
        user.name.value,
        user.surname.value,
        user.role,
        user.phone.value,
        user.email.value
      )
    }

    def readDeliveryDomainToDto(domain: DeliveryReadDomain): DeliveryReadDto = {
      DeliveryReadDto(
        domain.id.value,
        domain.orderId.value,
        readAuthorizedUserDomainToDto(domain.courier),
        domain.deliveryStartDate.value,
        domain.deliveryFinishDate.map(_.value)
      )
    }

    def readOrderDomainToDto(domain: OrderReadDomain): OrderReadDto = {
      val orderItems = domain.orderItems.map(orderItemDomainToDto)
      OrderReadDto(
        domain.id.value,
        domain.userId.value,
        orderItems,
        domain.orderStatus,
        domain.orderedStartDate.value,
        domain.total.value,
        domain.address.value
      )
    }

    def updateOrderDomainToDto(domain: OrderUpdateDomain): OrderUpdateDto = {
      OrderUpdateDto(domain.id.value, domain.orderStatus)
    }

    def createProductDomainToDto(product: ProductCreateDomain): ProductCreateDto = {
      ProductCreateDto(
        product.name.value,
        product.categoryId.value,
        product.supplierId.value,
        product.price.value,
        product.description
      )
    }

    def updateProductDomainToDto(product: ProductUpdateDomain): ProductUpdateDto = {
      ProductUpdateDto(
        product.id.value,
        product.name.value,
        product.categoryId.value,
        product.supplierId.value,
        product.price.value,
        product.description,
        product.status
      )
    }

    def readAttachmentDomainToDto(domain: AttachmentReadDomain): AttachmentReadDto = {
      AttachmentReadDto(domain.id.value, domain.attachment.value)
    }

    def readProductDomainToDto(product: ProductReadDomain): ProductReadDto = {
      ProductReadDto(
        product.id.value,
        product.name.value,
        categoryDomainToDto(product.category),
        supplierDomainToDto(product.supplier),
        product.price.value,
        product.description,
        product.status,
        product.publicationDate.value,
        product.attachments.map(readAttachmentDomainToDto)
      )
    }

    def attachmentDomainToDto(attachment: AttachmentCreateDomain): AttachmentCreateDto = {
      AttachmentCreateDto(
        attachment.attachment.value,
        attachment.productId.value
      )
    }

    def supplierDomainToDto(supplier: SupplierDomain): SupplierDto = {
      SupplierDto(
        supplier.id.value,
        supplier.name.value,
        supplier.address.value
      )
    }

    def categoryDomainToDto(domain: CategoryDomain): CategoryDto = {
      CategoryDto(
        domain.id.value,
        domain.name.value
      )
    }

    def orderItemDomainToDto(domain: OrderProductReadDomain): OrderProductDto = {
      OrderProductDto(domain.productId.value, domain.count.value)
    }
  }

  object DtoToDomain {

    def validateUserDto(dto: NonAuthorizedUserDto): ValidatedNec[GeneralError, NonAuthorizedUserDomain] = {
      val login:    ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation(dto.login)
      val password: ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation(dto.password)
      (
        login,
        password
      ).mapN(NonAuthorizedUserDomain)
    }

    def validateCreateDeliveryDto(dto: DeliveryCreateDto): ValidatedNec[GeneralError, DeliveryCreateDomain] = {
      val orderId: ValidatedNec[GeneralError, UuidStr] = refinedValidation(dto.orderId)
      orderId.map(DeliveryCreateDomain)
    }

    def validateCreateProductGroupDto(dto: GroupCreateDto): ValidatedNec[GeneralError, GroupCreateDomain] = {
      val name: ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation(dto.name)
      name.map(GroupCreateDomain)
    }

    def validateProductGroupWithUsersDto(
      dto: GroupWithUsersDto
    ): ValidatedNec[GeneralError, GroupWithUsersDomain] = {

      def validateId: ValidatedNec[GeneralError, UuidStr] = refinedValidation(dto.id)

      def checkDuplicates(userIds: List[UuidStr]): ValidatedNec[GeneralError, List[UuidStr]] = {
        val duplicatedIds = userIds.diff(userIds.distinct)
        if (duplicatedIds.isEmpty) userIds.validNec
        else duplicatedIds.traverse(id => DuplicatedUserInGroup(id.value).invalidNec)
      }

      def tryConvertToNel(userIds: List[UuidStr]): ValidatedNec[GeneralError, NonEmptyList[UuidStr]] = {
        userIds.toNel.toValidNec(EmptyGroup)
      }

      def validateUserIds: ValidatedNec[GeneralError, NonEmptyList[UuidStr]] = {
        val userIds: ValidatedNec[GeneralError, List[UuidStr]] = dto.userIds.traverse(id => refinedValidation(id))
        userIds.andThen(checkDuplicates).andThen(tryConvertToNel)
      }

      (
        validateId,
        validateUserIds
      ).mapN(GroupWithUsersDomain)
    }

    def validateProductGroupWithProductsDto(
      dto: GroupWithProductsDto
    ): ValidatedNec[GeneralError, GroupWithProductsDomain] = {

      def validateId: ValidatedNec[GeneralError, UuidStr] = refinedValidation(dto.id)

      def checkDuplicates(productIds: List[UuidStr]): ValidatedNec[GeneralError, List[UuidStr]] = {
        val duplicatedIds = productIds.diff(productIds.distinct)
        if (duplicatedIds.isEmpty) productIds.validNec
        else duplicatedIds.traverse(id => DuplicatedProductInGroup(id.value).invalidNec)
      }

      def tryConvertToNel(productIds: List[UuidStr]): ValidatedNec[GeneralError, NonEmptyList[UuidStr]] = {
        productIds.toNel.toValidNec(EmptyGroup)
      }

      def validateProductIds: ValidatedNec[GeneralError, NonEmptyList[UuidStr]] = {
        val productIds: ValidatedNec[GeneralError, List[UuidStr]] = dto.productIds.traverse(id => refinedValidation(id))
        productIds.andThen(checkDuplicates).andThen(tryConvertToNel)
      }

      (
        validateId,
        validateProductIds
      ).mapN(GroupWithProductsDomain)

    }

    def validateUpdateOrderDto(dto: OrderUpdateDto): ValidatedNec[GeneralError, OrderUpdateDomain] = {
      val id: ValidatedNec[GeneralError, UuidStr] = refinedValidation(dto.id)
      (
        id,
        dto.orderStatus.validNec
      ).mapN(OrderUpdateDomain)
    }

    def validateCreateProductDto(productDto: ProductCreateDto): ValidatedNec[GeneralError, ProductCreateDomain] = {
      val price: ValidatedNec[GeneralError, NonNegativeFloat] =
        refinedValidation(productDto.price)
      val name:     ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation(productDto.name)
      val supplier: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(productDto.supplierId)
      val category: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(productDto.categoryId)
      (
        name,
        category,
        supplier,
        price,
        productDto.description.traverse(_.validNec)
      ).mapN(ProductCreateDomain)
    }

    def validateUpdateProductDto(dto: ProductUpdateDto): ValidatedNec[GeneralError, ProductUpdateDomain] = {
      val id:         ValidatedNec[GeneralError, UuidStr]          = refinedValidation(dto.id)
      val name:       ValidatedNec[GeneralError, NonEmptyStr]      = refinedValidation(dto.name)
      val supplierId: ValidatedNec[GeneralError, PositiveInt]      = refinedValidation(dto.supplierId)
      val price:      ValidatedNec[GeneralError, NonNegativeFloat] = refinedValidation(dto.price)
      val category:   ValidatedNec[GeneralError, PositiveInt]      = refinedValidation(dto.categoryId)
      (
        id,
        name,
        category,
        supplierId,
        price,
        dto.description.validNec,
        dto.status.validNec
      ).mapN(ProductUpdateDomain)
    }

    def validateAttachmentDto(dto: AttachmentCreateDto): ValidatedNec[GeneralError, AttachmentCreateDomain] = {
      val attachment: ValidatedNec[GeneralError, UrlStr]  = refinedValidation(dto.attachment)
      val productId:  ValidatedNec[GeneralError, UuidStr] = refinedValidation(dto.productId)
      (
        attachment,
        productId
      ).mapN(AttachmentCreateDomain)
    }

    def validateCriteriaDto(dto: CriteriaDto): ValidatedNec[GeneralError, CriteriaDomain] = {
      val startDate: ValidatedNec[GeneralError, Option[DateTimeStr]] = dto.startDate.traverse(p => refinedValidation(p))
      val endDate:   ValidatedNec[GeneralError, Option[DateTimeStr]] = dto.endDate.traverse(p => refinedValidation(p))
      val minPrice: ValidatedNec[GeneralError, Option[NonNegativeFloat]] =
        dto.minPrice.traverse(p => refinedValidation(p))
      val maxPrice: ValidatedNec[GeneralError, Option[NonNegativeFloat]] =
        dto.maxPrice.traverse(p => refinedValidation(p))
      (
        dto.name.traverse(_.validNec),
        dto.categoryName.traverse(_.validNec),
        dto.description.traverse(_.validNec),
        dto.supplierName.traverse(_.validNec),
        minPrice,
        maxPrice,
        startDate,
        endDate
      ).mapN(CriteriaDomain)
    }

    def validateSupplierDto(dto: SupplierDto): ValidatedNec[GeneralError, SupplierDomain] = {
      val name:    ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation(dto.name)
      val address: ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation(dto.address)
      val id:      ValidatedNec[GeneralError, PositiveInt] = refinedValidation(dto.id)
      (
        id,
        name,
        address
      ).mapN(SupplierDomain)
    }

    def validateCategorySubscriptionDto(
      dto: CategorySubscriptionDto
    ): ValidatedNec[GeneralError, CategorySubscriptionDomain] = {
      val category: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(dto.categoryId)
      category.map(CategorySubscriptionDomain)
    }

    def validateSupplierSubscriptionDto(
      dto: SupplierSubscriptionDto
    ): ValidatedNec[GeneralError, SupplierSubscriptionDomain] = {
      val supplierId: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(dto.supplierId)
      supplierId.map(SupplierSubscriptionDomain)
    }

    def validateOrderItemDto(dto: OrderProductDto): ValidatedNec[GeneralError, OrderProductCreateDomain] = {
      val id:    ValidatedNec[GeneralError, UuidStr]     = refinedValidation(dto.productId)
      val count: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(dto.count)
      (
        id,
        count
      ).mapN(OrderProductCreateDomain)
    }

    def validateCreateOrderDto(dto: OrderCreateDto): ValidatedNec[GeneralError, OrderCreateDomain] = {

      def validateAddress: ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation(dto.address)

      def checkDuplicates(productIds: List[String]): ValidatedNec[GeneralError, List[String]] = {
        val duplicatedIds = productIds.diff(productIds.distinct)
        if (duplicatedIds.isEmpty) productIds.validNec
        else duplicatedIds.traverse(id => DuplicatedProductInGroup(id).invalidNec)
      }

      def tryConvertToNel(
        productIds: List[OrderProductCreateDomain]
      ): ValidatedNec[GeneralError, NonEmptyList[OrderProductCreateDomain]] = {
        productIds.toNel.toValidNec(EmptyGroup)
      }

      def validateOrderProducts: ValidatedNec[GeneralError, NonEmptyList[OrderProductCreateDomain]] = {
        val orderProducts: ValidatedNec[GeneralError, List[OrderProductCreateDomain]] =
          dto.orderItems.traverse(validateOrderItemDto)
        orderProducts.productL(checkDuplicates(dto.orderItems.map(_.productId))).andThen(tryConvertToNel)
      }

      (
        validateOrderProducts.map(_.toList),
        0f.validNec,
        validateAddress
      ).mapN(OrderCreateDomain)
    }
  }

  object DbModelMapper {

    def joinProductsWithAttachments(
      products:    List[ProductReadDbDomain],
      attachments: List[AttachmentReadDomain]
    ): List[ProductReadDomain] = {
      products.map(product =>
        ProductReadDomain(
          product.id,
          product.name,
          product.category,
          product.supplier,
          product.price,
          product.description,
          product.status,
          product.publicationDate,
          attachments.filter(_.productId == product.id)
        )
      )
    }

    def joinOrdersWithProducts(
      orders:        List[OrderReadDbDomain],
      orderProducts: List[OrderProductReadDomain]
    ): List[OrderReadDomain] = {
      orders.map(order =>
        OrderReadDomain(
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

    def joinGroupsWithUsersAndProducts(
      groups:   List[GroupReadDbDomain],
      users:    List[GroupUserDomain],
      products: List[GroupProductDomain]
    ): List[GroupReadDomain] = {
      groups.map(group =>
        GroupReadDomain(
          group.id,
          group.name,
          users.filter(_.groupId == group.id).map(_.userId),
          products.filter(_.groupId == group.id).map(_.productId)
        )
      )
    }
  }
}
