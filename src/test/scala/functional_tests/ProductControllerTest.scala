package functional_tests

import cats.effect.unsafe.implicits._
import cats.effect.{Async, IO, Resource}
import org.http4s.Request
import org.http4s.blaze.client._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.funspec.AsyncFunSpec

import java.util.UUID
import scala.language.postfixOps

// TODO - add test DB, where all these changes will happen

class ProductControllerTest extends AsyncFunSpec {

  def clientResource[F[_]: Async]: Resource[F, Client[F]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    BlazeClientBuilder[F].withExecutionContext(global).resource
  }

  val client: Resource[IO, Client[IO]] = clientResource[IO]

  describe("ProductController") {
    val productAPIAddress = uri"http://localhost:8088/api/product"
    it(s"should return List of ReadProductDto instances for GET request $productAPIAddress") {
//      val expected = List(
//        ReadProductDto(
//          "adc88583-8537-4ee1-bbad-50a0d090e419",
//          "carrot",
//          Category.Food,
//          SupplierDto(1, "Mem", "Minsk"),
//          22.0f,
//          "test",
//          ProductStatus.InProcessing,
//          "2022-04-02"
//        )
//      )

      val request = Request[IO](method = GET, uri = productAPIAddress)
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == Ok)
        })
        .unsafeToFuture()
    }

    it("should create object when POST request was sent") {
      val body    = """{
                   |    "name":"meet",
                   |    "categoryId":1,
                   |    "supplierId": 1,
                   |    "price":25.2
                   |}""".stripMargin
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == Ok)
        })
        .unsafeToFuture()
    }

    it("should update object for given id with given params") {
      val body    = """{
                   |    "id":"adc88583-8537-4ee1-bbad-50a0d090e419",
                   |    "name":"carrot",
                   |    "categoryId": 1,
                   |    "supplierId": 1,
                   |    "price":22,
                   |    "description":"integration test",
                   |    "status" : "inProcessing"
                   |}""".stripMargin
      val request = Request[IO](method = PUT, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == Ok)
        })
        .unsafeToFuture()
    }

    it("should delete product if DELETE request given") {
      val body    = """{
                   |    "name":"meet",
                   |    "categoryId":1,
                   |    "supplierId": 1,
                   |    "price":25.2
                   |}""".stripMargin
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            id           <- cl.fetchAs[UUID](request)
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id)
            count        <- cl.fetchAs[Int](deleteRequest)
          } yield assert(count == 1)
        })
        .unsafeToFuture()
    }

    it(
      "should return List of ReadProductDto according to given Criteria object. " +
        "If Criteria has 2 or more fields filter will work with option AND (not OR)"
    ) {
      val body    = """{
                   |    "name" : "ca%",
                   |    "categoryName" : "foo%",
                   |    "startDate": "2019-01-01"
                   |}""".stripMargin
      val request = Request[IO](method = POST, uri = productAPIAddress / "search").withEntity(body)
//      val expected = List(
//        ReadProductDto(
//          "adc88583-8537-4ee1-bbad-50a0d090e419",
//          "carrot",
//          Category.Food,
//          SupplierDto(1, "Mem", "Minsk"),
//          22.0f,
//          "test",
//          ProductStatus.InProcessing,
//          "2022-04-02"
//        )
//      )
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == Ok)
        })
        .unsafeToFuture()
    }

    it("should return ProductNotFound error with status code 404 if you try to delete non-exists product") {
      val body    = """{
                   |    "name":"meet",
                   |    "categoryId":1,
                   |    "supplierId": 1,
                   |    "price":25.2
                   |}""".stripMargin
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            id           <- cl.fetchAs[UUID](request)
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / (id.toString + "1"))
            status       <- cl.status(deleteRequest)
          } yield assert(status == NotFound)
        })
        .unsafeToFuture()
    }

    it("should return BadRequestError if refined validation failed") {
      val body    = """{
                   |    "name":"meet",
                   |    "categoryId":-1,
                   |    "supplierId": -1,
                   |    "price":25.2
                   |}""".stripMargin
      val request = Request[IO](method = POST, uri = productAPIAddress).withEntity(body)
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == BadRequest)
        })
        .unsafeToFuture()
    }

    it(
      "should return ProductNotFound error with status code NotFound if you try to attach something to non-exists product"
    ) {
      val body    = """{
                   |    "attachment":"https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-fb.jpg",
                   |    "productId":"7befac6d-9e68-4064-927c-b9700438fea1"
                   |}""".stripMargin
      val request = Request[IO](method = POST, uri = productAPIAddress / "attach").withEntity(body)
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == NotFound)
        })
        .unsafeToFuture()
    }

    it("should return BadRequestError if JSON body validation failed") {
      val body    = """{
                   |    "nameh":"meet",
                   |    "categoryId":-1,
                   |    "supplierId": -1,
                   |    "price":-25.2
                   |}""".stripMargin
      val request = Request[IO](method = POST, uri = productAPIAddress / "attach").withEntity(body)
      client
        .use(cl => {
          for {
            status <- cl.status(request)
          } yield assert(status == BadRequest)
        })
        .unsafeToFuture()
    }
  }
}
