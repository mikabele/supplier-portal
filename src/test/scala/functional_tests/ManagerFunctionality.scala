package functional_tests

import cats.effect.unsafe.implicits._
import cats.effect.{Async, IO, Resource}
import domain.category.Category
import domain.product.ProductStatus
import dto.attachment.{AttachmentCreateDto, AttachmentReadDto}
import dto.criteria.CriteriaDto
import dto.group.{GroupCreateDto, GroupReadDto, GroupWithProductsDto, GroupWithUsersDto}
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

import java.time.LocalDateTime
import java.util.{Date, UUID}
import scala.language.postfixOps
import java.time.format.DateTimeFormatter

class ManagerFunctionality extends AnyFunSpec {

  def clientResource[F[_]: Async]: Resource[F, Client[F]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    BlazeClientBuilder[F].withExecutionContext(global).resource
  }

  val client: Resource[IO, Client[IO]] = clientResource[IO]

  private val productAPIAddress = uri"http://localhost:8088/api/product"
  private val groupAPIAddress   = uri"http://localhost:8088/api/product_group"

  private val managerUserId = "e188ba42-501a-4841-a3d3-5a4bcfcaeb33"
  private val courierUserId = "34418133-92b6-46b1-bb3b-edf2a9af1413"
  private val clientUserId  = "aa32d3e1-526e-4d76-b545-cf7cd413af6d"
  private val supplierDto   = SupplierDto(1, "Mem", "Minsk")
  private val dtf           = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  private val dateNow       = dtf.format(LocalDateTime.now())

  describe("Manager") {
    it(s"can be able to create, update, read adn delete any products.") {
      val createProductBody = ProductCreateDto("testproduct", Category.Food, 1, 20f, None)
      val request =
        Request[IO](method = POST, uri = productAPIAddress).withEntity(createProductBody)
      client
        .use(cl => {
          for {
            res <- cl
              .run(request)
              .use {
                case Successful(r) =>
                  for {
                    id <- r.as[UUID]
                    updateProductBody = ProductUpdateDto(
                      id.toString,
                      "testproduct",
                      Category.Food,
                      1,
                      20f,
                      "test update",
                      ProductStatus.InProcessing
                    )
                    updateRequest   = Request[IO](method = PUT, uri = productAPIAddress).withEntity(updateProductBody)
                    _              <- cl.status(updateRequest)
                    getRequest      = Request[IO](method = GET, uri = productAPIAddress / managerUserId)
                    actualProducts <- cl.fetchAs[List[ProductReadDto]](getRequest)
                    expectedProducts = List(
                      ProductReadDto(
                        id.toString,
                        "testproduct",
                        Category.Food,
                        supplierDto,
                        20f,
                        "test update",
                        ProductStatus.InProcessing,
                        dateNow,
                        List.empty[AttachmentReadDto]
                      )
                    )
                    deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id)
                    _            <- cl.status(deleteRequest)
                  } yield assert(actualProducts == expectedProducts)

                case _ => fail()
              }
          } yield res
        })
        .unsafeRunSync()
    }

    it("should be able to add, view and remove attachments") {
      val createProductBody = ProductCreateDto("testproduct", Category.Food, 1, 20f, None)
      val createProductRequest =
        Request[IO](method = POST, uri = productAPIAddress).withEntity(createProductBody)
      client
        .use(cl => {
          for {
            id <- cl.fetchAs[UUID](createProductRequest)
            attachmentCreateBody = AttachmentCreateDto(
              "https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-fb.jpg",
              id.toString
            )
            createAttachmentRequest =
              Request[IO](method = POST, uri = productAPIAddress / "attachment").withEntity(attachmentCreateBody)
            attachmentId   <- cl.fetchAs[UUID](createAttachmentRequest)
            getRequest      = Request[IO](method = GET, uri = productAPIAddress / managerUserId)
            actualProducts <- cl.fetchAs[List[ProductReadDto]](getRequest)
            expectedProducts = List(
              ProductReadDto(
                id.toString,
                "testproduct",
                Category.Food,
                supplierDto,
                20f,
                "",
                ProductStatus.Available,
                dateNow,
                List(
                  AttachmentReadDto(
                    attachmentId.toString,
                    "https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-fb.jpg"
                  )
                )
              )
            )
            deleteAttachmentRequest = Request[IO](
              method = DELETE,
              uri    = productAPIAddress / "attachment" / attachmentId
            )
            _            <- cl.status(deleteAttachmentRequest)
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id)
            _            <- cl.status(deleteRequest)
          } yield assert(actualProducts == expectedProducts)
        })
        .unsafeRunSync()
    }

    it(
      "should be able to create/delete product groups," +
        " add/remove users/products in them." +
        " If client not exists in at least one group with product " +
        "then he won't be able to see this product"
    ) {
      val createProductBody = ProductCreateDto("testproduct", Category.Food, 1, 20f, None)
      val createProductRequest =
        Request[IO](method = POST, uri = productAPIAddress).withEntity(createProductBody)

      val createGroupBody    = GroupCreateDto("age18")
      val createGroupRequest = Request[IO](method = POST, uri = groupAPIAddress).withEntity(createGroupBody)
      client
        .use(cl => {
          for {
            id                          <- cl.fetchAs[UUID](createProductRequest)
            getRequest                   = Request[IO](method = GET, uri = productAPIAddress / clientUserId)
            actualProductsBeforeAccess  <- cl.fetchAs[List[ProductReadDto]](getRequest)
            expectedProductsBeforeAccess = List.empty[ProductReadDto]
            groupId                     <- cl.fetchAs[UUID](createGroupRequest)
            groupWithUser                = GroupWithUsersDto(groupId.toString, List(clientUserId))
            groupWithProduct             = GroupWithProductsDto(groupId.toString, List(id.toString))
            addUsersRequest              = Request[IO](method = POST, uri = groupAPIAddress / "users").withEntity(groupWithUser)
            s1                          <- cl.status(addUsersRequest)
            addProductsRequest = Request[IO](method = POST, uri = groupAPIAddress / "products")
              .withEntity(groupWithProduct)
            s2                        <- cl.status(addProductsRequest)
            actualProductsAfterAccess <- cl.fetchAs[List[ProductReadDto]](getRequest)
            expectedProductsAfterAccess = List(
              ProductReadDto(
                id.toString,
                "testproduct",
                Category.Food,
                supplierDto,
                20f,
                "",
                ProductStatus.Available,
                dateNow,
                List.empty[AttachmentReadDto]
              )
            )
            getGroupsRequest   = Request[IO](method = GET, uri = groupAPIAddress)
            actualGroups      <- cl.fetchAs[List[GroupReadDto]](getGroupsRequest)
            expectedGroups     = List(GroupReadDto(groupId.toString, "age18", List(clientUserId), List(id.toString)))
            deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId)
            s3                <- cl.status(deleteGroupRequest)
            deleteRequest      = Request[IO](method = DELETE, uri = productAPIAddress / id)
            s4                <- cl.status(deleteRequest)
          } yield {
            assert(actualProductsBeforeAccess == expectedProductsBeforeAccess)
            assert(actualProductsAfterAccess == expectedProductsAfterAccess)
            assert(actualGroups == expectedGroups)
            assert(s1.isSuccess && s2.isSuccess && s3.isSuccess && s4.isSuccess)
          }
        })
        .unsafeRunSync()
    }

    describe(
      "should get different errors:" +
        " - BadRequestError if validation failed, jsonbody parsing failed and so on; " +
        " - NotFoundError if product not found while update/delete, group not found while inserting/deleting users/products from group and so on"
    ) {
      it("get BadRequestError if validation failed") {
        val createProductBody = ProductCreateDto("testproduct", Category.Food, -1, 20f, None)
        val createProductRequest =
          Request[IO](method = POST, uri = productAPIAddress).withEntity(createProductBody)

        client
          .use(cl => {
            for {
              status <- cl.status(createProductRequest)
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

      it("should return ProductNotFound error with status code 404 if you try to delete non-existing product") {
        val invalidDeleteRequest =
          Request[IO](method = DELETE, uri = productAPIAddress / "7befac6d-9e68-4064-927c-b9700438fea1")

        client
          .use(cl => {
            for {
              status <- cl.status(invalidDeleteRequest)
            } yield assert(status == NotFound)
          })
          .unsafeRunSync()
      }

      it("should return NotFoundError if you try to update non-existing product") {
        val updateProductBody = ProductUpdateDto(
          "7befac6d-9e68-4064-927c-b9700438fea1",
          "testproduct",
          Category.Food,
          1,
          20f,
          "test update",
          ProductStatus.InProcessing
        )
        val updateRequest = Request[IO](method = PUT, uri = productAPIAddress).withEntity(updateProductBody)

        client
          .use(cl => {
            for {
              status <- cl.status(updateRequest)
            } yield assert(status == NotFound)
          })
          .unsafeRunSync()
      }

      it("should return NotFound error if you try to delete non-existing group") {
        val deleteGroupRequest =
          Request[IO](method = DELETE, uri = groupAPIAddress / "7befac6d-9e68-4064-927c-b9700438fea1")

        client
          .use(cl => {
            for {
              status <- cl.status(deleteGroupRequest)
            } yield assert(status == NotFound)
          })
          .unsafeRunSync()
      }

      it("should return BadRequestError if you passed duplicated user/product ids in add_users_to_group endpoint") {
        val createGroupBody    = GroupCreateDto("age18")
        val createGroupRequest = Request[IO](method = POST, uri = groupAPIAddress).withEntity(createGroupBody)
        client
          .use(cl => {
            for {
              groupId           <- cl.fetchAs[UUID](createGroupRequest)
              groupWithUser      = GroupWithUsersDto(groupId.toString, List(clientUserId, clientUserId))
              addUsersRequest    = Request[IO](method = POST, uri = groupAPIAddress / "users").withEntity(groupWithUser)
              s1                <- cl.status(addUsersRequest)
              deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId)
              s3                <- cl.status(deleteGroupRequest)
            } yield assert(s1 == BadRequest)
          })
          .unsafeRunSync()
      }

      it("should return NotFoundError if you try to add/remove users/products from group") {
        val groupWithUser   = GroupWithUsersDto("7befac6d-9e68-4064-927c-b9700438fea1", List(clientUserId))
        val addUsersRequest = Request[IO](method = POST, uri = groupAPIAddress / "users").withEntity(groupWithUser)
        client
          .use(cl => {
            for {
              status <- cl.status(addUsersRequest)
            } yield assert(status == NotFound)
          })
          .unsafeRunSync()
      }

      it("should return NotFoundError if user/product with you want to add in the group doesn't exist") {
        val createGroupBody    = GroupCreateDto("age18")
        val createGroupRequest = Request[IO](method = POST, uri = groupAPIAddress).withEntity(createGroupBody)
        client
          .use(cl => {
            for {
              groupId           <- cl.fetchAs[UUID](createGroupRequest)
              groupWithUser      = GroupWithUsersDto(groupId.toString, List("7befac6d-9e68-4064-927c-b9700438fea1"))
              addUsersRequest    = Request[IO](method = POST, uri = groupAPIAddress / "users").withEntity(groupWithUser)
              s1                <- cl.status(addUsersRequest)
              deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId)
              s3                <- cl.status(deleteGroupRequest)
            } yield assert(s1 == NotFound)
          })
          .unsafeRunSync()
      }
    }
  }
}
