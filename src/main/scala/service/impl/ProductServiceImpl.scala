//package service.impl
//
//import cats.Applicative
//import cats.data.{EitherT, ValidatedNec}
//import cats.syntax.all._
//import domain.product._
//import domain.user.Role
//import dto.attachment._
//import dto.criteria.CriteriaDto
//import dto.product._
//import dto.user.NonAuthorizedUserDto
//import dto.{attachment, criteria, product, user}
//import eu.timepit.refined.api.{Refined, Validate}
//import eu.timepit.refined.numeric._
//import repository.ProductRepository
//import service.ProductService
//import service.error.product
//import service.error.product.ProductError
//import service.error.product.ProductError._
//import util.RefinedValidator._
//import types._
//import eu.timepit.refined._
//import eu.timepit.refined.numeric._
//import eu.timepit.refined.string.MatchesRegex._
//import eu.timepit.refined.auto._
//
//// TODO - implement methods
//
//class ProductServiceImpl[F[_]: Applicative](
//  productRep: ProductRepository[F]
//) extends ProductService[F] {
//  override def addProduct(productDto: CreateProductDto): F[ValidatedNec[ProductError, ReadProductDto]] = {
//    val res = for {
//      product <- EitherT(validateCreateProductDto(productDto).pure[F])
//    } yield ()
//
//    res.value
//  }
//
//  override def updateProduct(productDto: UpdateProductDto): F[ValidatedNec[ProductError, ReadProductDto]] = ???
//
//  override def deleteProduct(id: Int): F[Either[ProductError, Unit]] = ???
//
//  override def readProducts(): F[Either[ProductError, List[ReadProductDto]]] = ???
//
//  override def attach(
//    attachmentDto: attachment.CreateAttachmentDto
//  ): F[ValidatedNec[ProductError, attachment.CreateAttachmentDto]] = ???
//
//  override def searchByCriteria(criteriaDto: CriteriaDto): F[ValidatedNec[ProductError, List[ReadProductDto]]] = ???
//
//  private def validateCreateProductDto(productDto: CreateProductDto): Either[ProductError, CreateProduct] = {
//    (
//      refinedValidation(productDto.name, InvalidNameFormat),
//      refinedValidation(productDto.categoryId, InvalidIdFormat),
//      refinedValidation(productDto.supplierId, InvalidIdFormat),
//      refinedValidation(productDto.price, InvalidPrice),
//      productDto.description.validNec
//    ).mapN(CreateProduct)
//  }
//}
