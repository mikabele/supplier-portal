package unit_tests

import cats.data.ValidatedNec
import cats.effect.IO
import cats.syntax.all._
import domain.category.Category
import domain.supplier.SupplierDomain
import dto.product.ProductCreateDto
import error.general.GeneralError
import logger.LogHandler
import org.apache.logging.log4j.{LogManager, Logger}
import org.scalamock.scalatest.MockFactory
import org.scalatest.funspec.AnyFunSpec
import repository.{OrderRepository, ProductRepository, SupplierRepository}
import service.ProductService
import types._
import util.ModelMapper.DtoToDomain._
import util.RefinedValidator.refinedValidation

import java.util.UUID

class ProductServiceTest extends AnyFunSpec with MockFactory {

  val phone:       ValidatedNec[GeneralError, PhoneStr]    = refinedValidation("+375293843595")
  val email:       ValidatedNec[GeneralError, EmailStr]    = refinedValidation("mikabele12@gmail.com")
  val uuid:        UUID                                    = UUID.randomUUID()
  val nonEmptyStr: ValidatedNec[GeneralError, NonEmptyStr] = refinedValidation("aaa")
  val uuidStr:     ValidatedNec[GeneralError, UuidStr]     = refinedValidation(uuid.toString)
  val posInt:      ValidatedNec[GeneralError, PositiveInt] = refinedValidation(1)

  val testSupplierDomain: ValidatedNec[GeneralError, SupplierDomain] =
    (posInt, nonEmptyStr, nonEmptyStr).mapN(SupplierDomain)
  val logger: Logger = LogManager.getLogger("root")
  val logHandler: LogHandler[IO] = LogHandler.of(
    (s: String) => logger.info(s).pure[IO],
    (s: String) => logger.debug(s).pure[IO],
    (s: String) => logger.error(s).pure[IO]
  )

  val productRepository:  ProductRepository[IO]  = stub[ProductRepository[IO]]
  val supplierRepository: SupplierRepository[IO] = stub[SupplierRepository[IO]]
  val orderRepository:    OrderRepository[IO]    = stub[OrderRepository[IO]]

  val productService: ProductService[IO] =
    ProductService.of(productRepository, supplierRepository, orderRepository, logHandler)

  describe("Add product") {
    it("shouldn't fail if everything is correct") {
      val validProductDto: ProductCreateDto = ProductCreateDto("test", Category.Food, 1, 20f, None)

      (supplierRepository.getById _)
        .when(posInt.toOption.get)
        .returns(IO(Option(testSupplierDomain.toOption.get)))
      (productRepository.checkUniqueProduct _)
        .when(*, *)
        .returns(IO.none)
      (productRepository.addProduct _)
        .when(*)
        .returns(IO(uuid))

      productService.addProduct(validProductDto).unsafeRunSync()

      inSequenceWithLogging {
        (supplierRepository.getById _).verify(*).once
        (productRepository.checkUniqueProduct _).verify(*, *).once
        (productRepository.addProduct _).verify(*).once
      }
    }
    it("should fail if you try to add product with the same name and supplier more than once") {
      val failedProductDto = ProductCreateDto("test", Category.Food, 10, 1f, None)
      val failedPosInt: ValidatedNec[GeneralError, PositiveInt] = refinedValidation(10)
      (supplierRepository.getById _)
        .when(failedPosInt.toOption.get)
        .returns(IO.none)

      productService.addProduct(failedProductDto).unsafeRunSync()

      inSequenceWithLogging {
        (supplierRepository.getById _).verify(*).once()
        (productRepository.checkUniqueProduct _).verify(*, *).never
      }
    }
    it("should fail if you try to insert the same product twice") {
      val validProductDto: ProductCreateDto = ProductCreateDto("test", Category.Food, 1, 20f, None)

      (supplierRepository.getById _)
        .when(posInt.toOption.get)
        .returns(IO(Option(testSupplierDomain.toOption.get)))
      (productRepository.checkUniqueProduct _)
        .when(validProductDto.name, validProductDto.supplierId)
        .returns(IO.none)
        .noMoreThanOnce()
      (productRepository.checkUniqueProduct _)
        .when(validProductDto.name, validProductDto.supplierId)
        .returns(IO(Some(null)))
        .anyNumberOfTimes()
      (productRepository.addProduct _)
        .when(*)
        .returns(IO(uuid))

      productService.addProduct(validProductDto).unsafeRunSync()
      productService.addProduct(validProductDto).unsafeRunSync()

      inAnyOrderWithLogging {
        (supplierRepository.getById _).verify(*).twice()
        (productRepository.checkUniqueProduct _).verify(validProductDto.name, validProductDto.supplierId).twice()
        (productRepository.addProduct _).verify(*).once
      }
    }
  }

}
