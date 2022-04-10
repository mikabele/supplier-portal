package functional_tests

import cats.effect.unsafe.implicits._
import cats.effect.{Async, IO, Resource}
import domain.category.Category
import domain.product.ProductStatus
import dto.attachment.{AttachmentCreateDto, AttachmentReadDto}
import dto.criteria.CriteriaDto
import dto.product.{ProductCreateDto, ProductReadDto, ProductUpdateDto}
import dto.supplier.SupplierDto
import io.circe.generic.auto._
import org.http4s.Request
import org.http4s.Status.Successful
import org.http4s.blaze.client._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.funspec.AnyFunSpec

import java.util.UUID
import scala.language.postfixOps

//TODO - rewrite tests
class ProductControllerTest extends AnyFunSpec {

  def clientResource[F[_]: Async]: Resource[F, Client[F]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    BlazeClientBuilder[F].withExecutionContext(global).resource
  }

  val client: Resource[IO, Client[IO]] = clientResource[IO]

  describe("ProductController") {
    val productAPIAddress = uri"http://localhost:8088/api/product"
    it(s"should return List of ReadProductDto instances for GET request $productAPIAddress") {
      val expected = List(
        ProductReadDto(
          "adc88583-8537-4ee1-bbad-50a0d090e419",
          "carrot",
          Category.Food,
          SupplierDto(1, "Mem", "Minsk"),
          22.0f,
          "integration test",
          ProductStatus.Available,
          "2022-04-02",
          List(
            AttachmentReadDto(
              "0d3a671d-8798-49da-9d38-34a6bcbe5a0a",
              "https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-fb.jpg"
            ),
            AttachmentReadDto(
              "8aba309d-efcf-43be-849d-8f337235ea1d",
              "https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-fb.jpg"
            ),
            AttachmentReadDto(
              "47f683ab-6e24-4476-a7c0-dcb386f041b3",
              "https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-jpg"
            )
          )
        )
      )

      val request = Request[IO](method = GET, uri = productAPIAddress)
      client
        .use(cl => {
          for {
            response <- cl.fetchAs[List[ProductReadDto]](request)
          } yield assert(response == expected)
        })
        .unsafeRunSync()
    }

    it("should create object when POST request was sent") {

      val body    = ProductCreateDto("meeta", Category.Food, 1, 25.2f, None)
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            res <- cl
              .run(request)
              .use {
                case Successful(r) =>
                  for {
                    id           <- r.as[UUID]
                    deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id)
                    _            <- cl.status(deleteRequest)
                  } yield succeed

                case _ => fail()
              }
          } yield res
        })
        .unsafeRunSync()
    }

    it("should update object for given id with given params") {
      val body    = ProductCreateDto("meeta", Category.Food, 1, 25.2f, None)
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            res <- cl
              .run(request)
              .use {
                case Successful(r) =>
                  for {
                    id <- r.as[UUID]
                    b = ProductUpdateDto(
                      id.toString,
                      "meeta",
                      Category.Food,
                      1,
                      22.0f,
                      "integration_test",
                      ProductStatus.InProcessing
                    )
                    updateRequest = Request[IO](method = PUT, uri = productAPIAddress).withEntity(b)
                    response     <- cl.fetchAs[ProductUpdateDto](updateRequest)
                    deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id)
                    _            <- cl.status(deleteRequest)
                  } yield assert(response == b)

                case _ => fail()
              }
          } yield res
        })
        .unsafeRunSync()
    }

    it("should delete product if DELETE request given") {
      val body    = ProductCreateDto("meeta", Category.Food, 1, 25.2f, None)
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            id           <- cl.fetchAs[UUID](request)
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id)
            count        <- cl.fetchAs[Int](deleteRequest)
          } yield assert(count == 1)
        })
        .unsafeRunSync()
    }

    it(
      "should return List of ReadProductDto according to given Criteria object. " +
        "If Criteria has 2 or more fields filter will work with option AND (not OR)"
    ) {
      val body    = CriteriaDto(name = Some("ca%"), categoryName = Some("foo%"))
      val request = Request[IO](method = POST, uri = productAPIAddress / "search").withEntity(body)
      val expected = List(
        ProductReadDto(
          "adc88583-8537-4ee1-bbad-50a0d090e419",
          "carrot",
          Category.Food,
          SupplierDto(1, "Mem", "Minsk"),
          22.0f,
          "integration test",
          ProductStatus.Available,
          "2022-04-02",
          List(
            AttachmentReadDto(
              "0d3a671d-8798-49da-9d38-34a6bcbe5a0a",
              "https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-fb.jpg"
            ),
            AttachmentReadDto(
              "8aba309d-efcf-43be-849d-8f337235ea1d",
              "https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-fb.jpg"
            ),
            AttachmentReadDto(
              "47f683ab-6e24-4476-a7c0-dcb386f041b3",
              "https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-jpg"
            )
          )
        )
      )
      client
        .use(cl => {
          for {
            response <- cl.fetchAs[List[ProductReadDto]](request)
          } yield assert(response == expected)
        })
        .unsafeRunSync()
    }

    it("should return ProductNotFound error with status code 404 if you try to delete non-exists product") {
      val body    = ProductCreateDto("meeta", Category.Food, 1, 25.2f, None)
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            id                  <- cl.fetchAs[UUID](request)
            invalidDeleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / (id.toString + "1"))
            status              <- cl.status(invalidDeleteRequest)
            validDeleteRequest   = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
            _                   <- cl.status(validDeleteRequest)
          } yield assert(status == NotFound)
        })
        .unsafeRunSync()
    }

    it("should return BadRequestError if refined validation failed") {
      val body    = ProductCreateDto("meeta", Category.Food, -1, -25.2f, None)
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == BadRequest)
        })
        .unsafeRunSync()
    }

    it(
      "should return ProductNotFound error with status code NotFound if you try to attach something to non-exists product"
    ) {
      val body = AttachmentCreateDto(
        "https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-fb.jpg",
        "7befac6d-9e68-4064-927c-b9700438fea1"
      )
      val request = Request[IO](method = POST, uri = productAPIAddress / "attachment").withEntity(body)
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == NotFound)
        })
        .unsafeRunSync()
    }

    it("should return BadRequestError if JSON body validation failed") {
      val body    = """{
                   |    "nameh":"meetg",
                   |    "category":-1,
                   |    "supplierId": -1,
                   |    "price":-25.2
                   |}""".stripMargin
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == BadRequest)
        })
        .unsafeRunSync()
    }

    it("should return BadRequest if Category doesn't exists") {
      val body    = """{
                   |    "name":"meetj",
                   |    "category":10,
                   |    "supplierId": 1,
                   |    "price":25.2
                   |}""".stripMargin
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == BadRequest)
        })
        .unsafeRunSync()
    }

    it("should return NotFoundError if Supplier doesn't exists") {
      val body    = ProductCreateDto("meeta", Category.Food, 10, 25.2f, None)
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == NotFound)
        })
        .unsafeRunSync()
    }

    it("should return NotFoundError if you try to remove non-exists attachment") {
      val request =
        Request[IO](method = DELETE, uri = productAPIAddress / "attachment" / "3e05430f-a11e-4641-815b-5d15b1c85099")
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == NotFound)
        })
        .unsafeRunSync()
    }
  }
}
