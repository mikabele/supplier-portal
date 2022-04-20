package functional_tests

import cats.effect.{Async, ConcurrentEffect, ContextShift, IO, Resource, Timer}
import domain.category.Category
import domain.product.ProductStatus
import dto.attachment.{AttachmentCreateDto, AttachmentReadDto}
import dto.group.{GroupCreateDto, GroupReadDto, GroupWithProductsDto, GroupWithUsersDto}
import dto.order.{OrderCreateDto, OrderProductDto}
import dto.product.{ProductCreateDto, ProductReadDto, ProductUpdateDto}
import dto.supplier.SupplierDto
import dto.user.NonAuthorizedUserDto
import io.circe.generic.auto._
import org.http4s.blaze.client._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{Request, ResponseCookie}
import org.scalatest.BeforeAndAfter
import org.scalatest.funspec.AnyFunSpec

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps

class ManagerFunctionality extends AnyFunSpec with BeforeAndAfter {

  def clientResource[F[_]: Async: ConcurrentEffect]: Resource[F, Client[F]] = {
    BlazeClientBuilder[F](global).resource
  }

  implicit val cs:    ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]        = IO.timer(global)

  val client: Resource[IO, Client[IO]] = clientResource[IO]

  private val productAPIAddress = uri"http://localhost:8088/api/product"
  private val groupAPIAddress   = uri"http://localhost:8088/api/product_group"
  private val authAPIAddress    = uri"http://localhost:8088/api/auth"
  private val orderAPIAddress   = uri"http://localhost:8088/api/order"

  private val managerUser = NonAuthorizedUserDto("mikabele", "1234")
  private val courierUser = NonAuthorizedUserDto("mikabele_courier", "1234")
  private val clientUser  = NonAuthorizedUserDto("mikabele_client", "1234")

  def getCookie(user: NonAuthorizedUserDto): ResponseCookie = {
    val request = Request[IO](method = POST, uri = authAPIAddress).withEntity(user)
    client
      .use(cl => {
        for {
          cookie <- cl.run(request).use(response => IO(response.cookies.head))

        } yield cookie
      })
      .unsafeRunSync()
  }

  private val managerCookie = getCookie(managerUser)
  private val clientCookie  = getCookie(clientUser)
  private val courierCookie = getCookie(courierUser)

  private val clientUserId = clientCookie.content.split("-", 3).last

  private val supplierDto = SupplierDto(1, "Mem", "Minsk")
  private val dtf         = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
  private val dateNow     = dtf.format(LocalDateTime.now())

  describe("Manager") {
    it(s"can be able to create, update, read adn delete any products.") {
      val createProductBody = ProductCreateDto("testproduct", Category.Food, 1, 20f, None)
      val request =
        Request[IO](method = POST, uri = productAPIAddress)
          .withEntity(createProductBody)
          .addCookie(managerCookie.name, managerCookie.content)
      client
        .use(cl => {
          for {
            id <- cl.fetchAs[UUID](request)
            updateProductBody = ProductUpdateDto(
              id.toString,
              "testproduct",
              Category.Food,
              1,
              20f,
              "test update",
              ProductStatus.InProcessing
            )
            updateRequest = Request[IO](method = PUT, uri = productAPIAddress)
              .withEntity(updateProductBody)
              .addCookie(managerCookie.name, managerCookie.content)
            s1 <- cl.status(updateRequest)
            getRequest = Request[IO](method = GET, uri = productAPIAddress)
              .addCookie(managerCookie.name, managerCookie.content)
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
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
              .addCookie(managerCookie.name, managerCookie.content)
            s2 <- cl.status(deleteRequest)
          } yield {
            assert(actualProducts.map(_.copy(publicationDate = dateNow)) == expectedProducts)
            assert(s1.isSuccess && s2.isSuccess)
          }
        })
        .unsafeRunSync()
    }

    it("should be able to add, view and remove attachments") {
      val createProductBody = ProductCreateDto("testproduct", Category.Food, 1, 20f, None)
      val createProductRequest =
        Request[IO](method = POST, uri = productAPIAddress)
          .withEntity(createProductBody)
          .addCookie(managerCookie.name, managerCookie.content)
      client
        .use(cl => {
          for {
            id <- cl.fetchAs[UUID](createProductRequest)
            attachmentCreateBody = AttachmentCreateDto(
              "https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-fb.jpg",
              id.toString
            )
            createAttachmentRequest =
              Request[IO](method = POST, uri = productAPIAddress / "attachment")
                .withEntity(attachmentCreateBody)
                .addCookie(managerCookie.name, managerCookie.content)
            attachmentId <- cl.fetchAs[UUID](createAttachmentRequest)
            getRequest = Request[IO](method = GET, uri = productAPIAddress)
              .addCookie(managerCookie.name, managerCookie.content)
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
              uri    = productAPIAddress / "attachment" / attachmentId.toString
            ).addCookie(managerCookie.name, managerCookie.content)
            s1 <- cl.status(deleteAttachmentRequest)
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
              .addCookie(managerCookie.name, managerCookie.content)
            s2 <- cl.status(deleteRequest)
          } yield {
            assert(actualProducts.map(_.copy(publicationDate = dateNow)) == expectedProducts)
            assert(s1.isSuccess && s2.isSuccess)
          }
        })
        .unsafeRunSync()
    }

    it(
      "should be able to create/delete product groups," +
        " add/remove users/products in them." +
        " If client not exists in at least one group with product " +
        "then he won't be able to see this product . If product is not at any group than all clients will see this product"
    ) {
      val createProductBody = ProductCreateDto("testproduct", Category.Food, 1, 20f, None)
      val createProductRequest =
        Request[IO](method = POST, uri = productAPIAddress)
          .withEntity(createProductBody)
          .addCookie(managerCookie.name, managerCookie.content)

      val createGroupBody = GroupCreateDto("age18")
      val createGroupRequest = Request[IO](method = POST, uri = groupAPIAddress)
        .withEntity(createGroupBody)
        .addCookie(managerCookie.name, managerCookie.content)

      val emptyList = List.empty[ProductReadDto]
      val getRequest = Request[IO](method = GET, uri = productAPIAddress)
        .addCookie(clientCookie.name, clientCookie.content)
      client
        .use(cl => {
          for {
            id <- cl.fetchAs[UUID](createProductRequest)
            nonEmptyList = List(
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
            productList1    <- cl.fetchAs[List[ProductReadDto]](getRequest)
            groupId         <- cl.fetchAs[UUID](createGroupRequest)
            groupWithUser    = GroupWithUsersDto(groupId.toString, List(clientUserId))
            groupWithProduct = GroupWithProductsDto(groupId.toString, List(id.toString))
            addProductsRequest = Request[IO](method = POST, uri = groupAPIAddress / "products")
              .addCookie(managerCookie.name, managerCookie.content)
              .withEntity(groupWithProduct)
            s2           <- cl.status(addProductsRequest)
            productList2 <- cl.fetchAs[List[ProductReadDto]](getRequest)

            addUsersRequest = Request[IO](method = POST, uri = groupAPIAddress / "users")
              .withEntity(groupWithUser)
              .addCookie(managerCookie.name, managerCookie.content)
            s1           <- cl.status(addUsersRequest)
            productList3 <- cl.fetchAs[List[ProductReadDto]](getRequest)
            getGroupsRequest = Request[IO](method = GET, uri = groupAPIAddress)
              .addCookie(managerCookie.name, managerCookie.content)
            actualGroups  <- cl.fetchAs[List[GroupReadDto]](getGroupsRequest)
            expectedGroups = List(GroupReadDto(groupId.toString, "age18", List(clientUserId), List(id.toString)))
            deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId.toString)
              .addCookie(managerCookie.name, managerCookie.content)
            s3 <- cl.status(deleteGroupRequest)
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
              .addCookie(managerCookie.name, managerCookie.content)
            s4 <- cl.status(deleteRequest)
          } yield {
            assert(productList1.map(_.copy(publicationDate = dateNow)) == nonEmptyList)
            assert(productList2.map(_.copy(publicationDate = dateNow)) == emptyList)
            assert(productList3.map(_.copy(publicationDate = dateNow)) == nonEmptyList)
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
          Request[IO](method = POST, uri = productAPIAddress)
            .withEntity(createProductBody)
            .addCookie(managerCookie.name, managerCookie.content)

        client
          .use(cl => {
            for {
              status <- cl.status(createProductRequest)
            } yield assert(status == BadRequest)
          })
          .unsafeRunSync()
      }

      it("should return BadRequest if Category doesn't exists") {
        val body = """{
                     |    "name":"meetj",
                     |    "category":10,
                     |    "supplierId": 1,
                     |    "price":25.2
                     |}""".stripMargin
        val request = Request[IO](method = POST, uri = productAPIAddress)
          .withEntity(body)
          .addCookie(managerCookie.name, managerCookie.content)
        client
          .use(cl => {
            for {
              status <- cl.status(request)
            } yield assert(status == BadRequest)
          })
          .unsafeRunSync()
      }

      it("should return NotFoundError if Supplier doesn't exists") {
        val body = ProductCreateDto("meeta", Category.Food, 10, 25.2f, None)
        val request = Request[IO](method = POST, uri = productAPIAddress)
          .withEntity(body)
          .addCookie(managerCookie.name, managerCookie.content)
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
            .addCookie(managerCookie.name, managerCookie.content)
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
        val request = Request[IO](method = POST, uri = productAPIAddress / "attachment")
          .withEntity(body)
          .addCookie(managerCookie.name, managerCookie.content)
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
            .addCookie(managerCookie.name, managerCookie.content)

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
        val updateRequest = Request[IO](method = PUT, uri = productAPIAddress)
          .withEntity(updateProductBody)
          .addCookie(managerCookie.name, managerCookie.content)

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
            .addCookie(managerCookie.name, managerCookie.content)

        client
          .use(cl => {
            for {
              status <- cl.status(deleteGroupRequest)
            } yield assert(status == NotFound)
          })
          .unsafeRunSync()
      }

      it("should return BadRequestError if you passed duplicated user/product ids in add_users_to_group endpoint") {
        val createGroupBody = GroupCreateDto("age18")
        val createGroupRequest = Request[IO](method = POST, uri = groupAPIAddress)
          .withEntity(createGroupBody)
          .addCookie(managerCookie.name, managerCookie.content)
        client
          .use(cl => {
            for {
              groupId      <- cl.fetchAs[UUID](createGroupRequest)
              groupWithUser = GroupWithUsersDto(groupId.toString, List(clientUserId, clientUserId))
              addUsersRequest = Request[IO](method = POST, uri = groupAPIAddress / "users")
                .withEntity(groupWithUser)
                .addCookie(managerCookie.name, managerCookie.content)
              s1 <- cl.status(addUsersRequest)
              deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId.toString)
                .addCookie(managerCookie.name, managerCookie.content)
              s3 <- cl.status(deleteGroupRequest)
            } yield {
              assert(s1 == BadRequest)
              assert(s3.isSuccess)
            }
          })
          .unsafeRunSync()
      }

      it("should return NotFoundError if you try to add/remove users/products from group") {
        val groupWithUser = GroupWithUsersDto("7befac6d-9e68-4064-927c-b9700438fea1", List(clientUserId))
        val addUsersRequest = Request[IO](method = POST, uri = groupAPIAddress / "users")
          .withEntity(groupWithUser)
          .addCookie(managerCookie.name, managerCookie.content)
        client
          .use(cl => {
            for {
              status <- cl.status(addUsersRequest)
            } yield assert(status == NotFound)
          })
          .unsafeRunSync()
      }

      it("should return NotFoundError if user/product with you want to add in the group doesn't exist") {
        val createGroupBody = GroupCreateDto("age18")
        val createGroupRequest = Request[IO](method = POST, uri = groupAPIAddress)
          .withEntity(createGroupBody)
          .addCookie(managerCookie.name, managerCookie.content)
        client
          .use(cl => {
            for {
              groupId      <- cl.fetchAs[UUID](createGroupRequest)
              groupWithUser = GroupWithUsersDto(groupId.toString, List("7befac6d-9e68-4064-927c-b9700438fea1"))
              addUsersRequest = Request[IO](method = POST, uri = groupAPIAddress / "users")
                .withEntity(groupWithUser)
                .addCookie(managerCookie.name, managerCookie.content)
              s1 <- cl.status(addUsersRequest)
              deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId.toString)
                .addCookie(managerCookie.name, managerCookie.content)
              s3 <- cl.status(deleteGroupRequest)
            } yield {
              assert(s1 == NotFound)
              assert(s3.isSuccess)
            }
          })
          .unsafeRunSync()
      }

      it(
        "should get ForbiddenError if manager will try to delete product in active order(not cancelled and not delivered)"
      ) {
        val createProductBody = ProductCreateDto("testproduct", Category.Food, 1, 20f, None)
        val request =
          Request[IO](method = POST, uri = productAPIAddress)
            .withEntity(createProductBody)
            .addCookie(managerCookie.name, managerCookie.content)
        val createGroupBody = GroupCreateDto("age18")
        val createGroupRequest = Request[IO](method = POST, uri = groupAPIAddress)
          .withEntity(createGroupBody)
          .addCookie(managerCookie.name, managerCookie.content)
        client
          .use(cl => {
            for {
              id              <- cl.fetchAs[UUID](request)
              groupId         <- cl.fetchAs[UUID](createGroupRequest)
              groupWithUser    = GroupWithUsersDto(groupId.toString, List(clientUserId))
              groupWithProduct = GroupWithProductsDto(groupId.toString, List(id.toString))
              addUsersRequest = Request[IO](method = POST, uri = groupAPIAddress / "users")
                .withEntity(groupWithUser)
                .addCookie(managerCookie.name, managerCookie.content)
              s1 <- cl.status(addUsersRequest)
              addProductsRequest = Request[IO](method = POST, uri = groupAPIAddress / "products")
                .addCookie(managerCookie.name, managerCookie.content)
                .withEntity(groupWithProduct)
              s2           <- cl.status(addProductsRequest)
              makeOrderBody = OrderCreateDto(List(OrderProductDto(id.toString, 10)), "Minsk")
              makeOrderRequest = Request[IO](method = POST, uri = orderAPIAddress)
                .addCookie(clientCookie.name, clientCookie.content)
                .withEntity(makeOrderBody)
              orderId <- cl.fetchAs[UUID](makeOrderRequest)
              viewOrdersRequest = Request[IO](method = GET, uri = orderAPIAddress)
                .addCookie(clientCookie.name, clientCookie.content)
              status <- cl.status(viewOrdersRequest)

              deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
                .addCookie(managerCookie.name, managerCookie.content)
              invalidStatus <- cl.status(deleteRequest)

              cancelOrderRequest = Request[IO](method = PUT, uri = orderAPIAddress / orderId.toString)
                .addCookie(clientCookie.name, clientCookie.content)
              s3 <- cl.status(cancelOrderRequest)
              deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId.toString)
                .addCookie(managerCookie.name, managerCookie.content)
              s4 <- cl.status(deleteGroupRequest)
              s5 <- cl.status(deleteRequest)
            } yield {
              assert(status.isSuccess && s1.isSuccess && s2.isSuccess && s3.isSuccess && s4.isSuccess && s5.isSuccess)
              assert(invalidStatus == Forbidden)
            }
          })
          .unsafeRunSync()
      }
    }
  }
}
