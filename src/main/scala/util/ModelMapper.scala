package util

import domain.attachment._
import domain.product._
import dto.attachment._
import dto.product._

import io.scalaland.chimney.dsl._

object ModelMapper {
  // TODO - replace ??? with implementation of methods (right now i just need the declaration of this methods)
  // TODO - read about scalaland lib

  def createProductDomainToDto(product: CreateProduct): CreateProductDto = product.into[CreateProductDto].transform
  def updateProductDomainToDto(product: UpdateProduct): UpdateProductDto = product.into[UpdateProductDto].transform
  def readProductDomainToDto(product:   ReadProduct): ReadProductDto = product.into[ReadProductDto].transform
  def attachmentDomainToDto(attachment: CreateAttachment): CreateAttachmentDto =
    attachment.into[CreateAttachmentDto].transform
}
