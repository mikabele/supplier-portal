package functional_tests

import cats.effect._
import domain.category.Category
import domain.product.ProductStatus
import dto.attachment.{AttachmentCreateDto, AttachmentReadDto}
import dto.criteria.CriteriaDto
import dto.delivery.DeliveryCreateDto
import dto.group.{GroupCreateDto, GroupWithProductsDto, GroupWithUsersDto}
import dto.order.{OrderCreateDto, OrderProductDto}
import dto.product.{ProductCreateDto, ProductReadDto, ProductUpdateDto}
import dto.subscription.{CategorySubscriptionDto, SupplierSubscriptionDto}
import dto.supplier.SupplierDto
import dto.user.NonAuthorizedUserDto
import io.circe.generic.auto._
import org.http4s.blaze.client._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.{Request, ResponseCookie}
import org.scalatest.funspec.AnyFunSpec

import scala.concurrent.ExecutionContext.Implicits.global
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class ClientFunctionality extends AnyFunSpec {
  def clientResource[F[_]: Async: ConcurrentEffect]: Resource[F, Client[F]] = {
    BlazeClientBuilder[F](global).resource
  }

  implicit val cs:    ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]        = IO.timer(global)

  val client: Resource[IO, Client[IO]] = clientResource[IO]

  private val productAPIAddress      = uri"http://localhost:8088/api/product"
  private val groupAPIAddress        = uri"http://localhost:8088/api/product_group"
  private val authAPIAddress         = uri"http://localhost:8088/api/auth"
  private val orderAPIAddress        = uri"http://localhost:8088/api/order"
  private val deliveryAPIAddress     = uri"http://localhost:8088/api/delivery"
  private val subscriptionAPIAddress = uri"http://localhost:8088/api/subscription"

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

  describe("Client") {
    it(
      "should be able browse and search products by different criterias. He can see only Available and NotAvailable Products. User should be in at least one group where product is. Otherwise, he can't see product"
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
      val criteria = CriteriaDto(minPrice = Some(10f))
      val criteriaRequest =
        Request[IO](method = POST, uri = productAPIAddress / "search")
          .withEntity(criteria)
          .addCookie(clientCookie.name, clientCookie.content)
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
            s2 <- cl.status(addProductsRequest)
            getRequest = Request[IO](method = GET, uri = productAPIAddress)
              .addCookie(clientCookie.name, clientCookie.content)
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
                List.empty[AttachmentReadDto]
              )
            )

            actualProductsByCriteria <- cl.fetchAs[List[ProductReadDto]](criteriaRequest)
            deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId.toString)
              .addCookie(managerCookie.name, managerCookie.content)
            s3 <- cl.status(deleteGroupRequest)
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
              .addCookie(managerCookie.name, managerCookie.content)
            s4 <- cl.status(deleteRequest)
          } yield {
            assert(actualProducts.map(_.copy(publicationDate = dateNow)) == expectedProducts)
            assert(actualProductsByCriteria.map(_.copy(publicationDate = dateNow)) == expectedProducts)
            assert(s1.isSuccess && s2.isSuccess && s3.isSuccess && s4.isSuccess)
          }
        })
        .unsafeRunSync()
    }

    it(
      "should be able to make order, view his orders, cancel his order. Can add only available products in orderDto"
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

            cancelOrderRequest = Request[IO](method = PUT, uri = orderAPIAddress / orderId.toString)
              .addCookie(clientCookie.name, clientCookie.content)
            s3 <- cl.status(cancelOrderRequest)
            deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId.toString)
              .addCookie(managerCookie.name, managerCookie.content)
            s4 <- cl.status(deleteGroupRequest)
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
              .addCookie(managerCookie.name, managerCookie.content)
            s5 <- cl.status(deleteRequest)
          } yield {
            assert(status.isSuccess && s1.isSuccess && s2.isSuccess && s3.isSuccess && s4.isSuccess && s5.isSuccess)
          }
        })
        .unsafeRunSync()
    }

    it("should be able to subscribe category/supplier , view his subscriptions, remove subscription") {
      val subscribeCategoryBody = CategorySubscriptionDto(Category.Food)
      val subscribeCategoryRequest =
        Request[IO](method = POST, uri = subscriptionAPIAddress / "category")
          .addCookie(clientCookie.name, clientCookie.content)
          .withEntity(subscribeCategoryBody)
      val subscribeSupplierBody = SupplierSubscriptionDto(1)
      val subscribeSupplierRequest =
        Request[IO](method = POST, uri = subscriptionAPIAddress / "supplier")
          .addCookie(clientCookie.name, clientCookie.content)
          .withEntity(subscribeSupplierBody)

      val getSupplierSubRequest = Request[IO](method = GET, uri = subscriptionAPIAddress / "supplier")
        .addCookie(clientCookie.name, clientCookie.content)
      val getCategorySubRequest = Request[IO](method = GET, uri = subscriptionAPIAddress / "category")
        .addCookie(clientCookie.name, clientCookie.content)
      val removeSupplierSubRequest =
        Request[IO](method = DELETE, uri = subscriptionAPIAddress / "supplier")
          .addCookie(clientCookie.name, clientCookie.content)
          .withEntity(subscribeSupplierBody)
      val removeCategorySubRequest =
        Request[IO](method = DELETE, uri = subscriptionAPIAddress / "category")
          .addCookie(clientCookie.name, clientCookie.content)
          .withEntity(subscribeCategoryBody)

      val createProductBody = ProductCreateDto("testproduct", Category.Food, 1, 20f, None)
      val createProductRequest =
        Request[IO](method = POST, uri = productAPIAddress)
          .withEntity(createProductBody)
          .addCookie(managerCookie.name, managerCookie.content)
      client
        .use(cl => {
          for {
            s1                <- cl.status(subscribeCategoryRequest)
            s2                <- cl.status(subscribeSupplierRequest)
            actualCategories  <- cl.fetchAs[List[Category]](getCategorySubRequest)
            expectedCategories = List(Category.Food)
            actualSuppliers   <- cl.fetchAs[List[SupplierDto]](getSupplierSubRequest)
            expectedSuppliers  = List(supplierDto)
            id                <- cl.fetchAs[UUID](createProductRequest)
            attachmentCreateBody = AttachmentCreateDto(
              "https://upload.wikimedia.org/wikipedia/commons/d/dc/Carrot-fb.jpg",
              id.toString
            )
            createAttachmentRequest =
              Request[IO](method = POST, uri = productAPIAddress / "attachment")
                .withEntity(attachmentCreateBody)
                .addCookie(managerCookie.name, managerCookie.content)
            attachmentId <- cl.fetchAs[UUID](createAttachmentRequest)
            _             = Thread.sleep(120000) // check email
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
              .addCookie(managerCookie.name, managerCookie.content)
            s2 <- cl.status(deleteRequest)
            s3 <- cl.status(removeSupplierSubRequest)
            s4 <- cl.status(removeCategorySubRequest)
          } yield {
            assert(actualSuppliers == expectedSuppliers)
            assert(actualCategories == expectedCategories)
            assert(s1.isSuccess && s2.isSuccess && s3.isSuccess && s4.isSuccess)
          }
        })
        .unsafeRunSync()
    }

    describe("order limitations") {
      it("should return NotFoundError if you try to cancel non-existing order") {
        val cancelOrderRequest =
          Request[IO](method = PUT, uri = orderAPIAddress / "7befac6d-9e68-4064-927c-b9700438fea1")
            .addCookie(clientCookie.name, clientCookie.content)
        client
          .use(cl => {
            for {
              status <- cl.status(cancelOrderRequest)
            } yield assert(status == NotFound)
          })
          .unsafeRunSync()
      }

      it("should return BadRequestError if you try to cancel order that is in status Cancelled, Assigned, Delivered") {
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
              orderId           <- cl.fetchAs[UUID](makeOrderRequest)
              createDeliveryBody = DeliveryCreateDto(orderId.toString)
              createDeliveryRequest = Request[IO](method = POST, uri = deliveryAPIAddress)
                .addCookie(courierCookie.name, courierCookie.content)
                .withEntity(createDeliveryBody)
              s3 <- cl.status(createDeliveryRequest)
              cancelOrderRequest = Request[IO](method = PUT, uri = orderAPIAddress / orderId.toString)
                .addCookie(clientCookie.name, clientCookie.content)
              status <- cl.status(cancelOrderRequest)
              deliveredRequest = Request[IO](method = PUT, uri = deliveryAPIAddress / orderId.toString)
                .addCookie(courierCookie.name, courierCookie.content)
              s6 <- cl.status(deliveredRequest)
              deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId.toString)
                .addCookie(managerCookie.name, managerCookie.content)
              s4 <- cl.status(deleteGroupRequest)
              deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
                .addCookie(managerCookie.name, managerCookie.content)
              s5 <- cl.status(deleteRequest)
            } yield {
              assert(s1.isSuccess && s2.isSuccess && s3.isSuccess && s6.isSuccess && s4.isSuccess && s5.isSuccess)
              assert(status == BadRequest)
            }
          })
          .unsafeRunSync()
      }

      it("should return BadRequestError if there are duplicated product ids in order") {
        val makeOrderBody = OrderCreateDto(
          List(
            OrderProductDto("7befac6d-9e68-4064-927c-b9700438fea1", 10),
            OrderProductDto("7befac6d-9e68-4064-927c-b9700438fea1", 10)
          ),
          "Minsk"
        )
        val makeOrderRequest = Request[IO](method = POST, uri = orderAPIAddress)
          .addCookie(clientCookie.name, clientCookie.content)
          .withEntity(makeOrderBody)
        client.use(cl => {
          for {
            status <- cl.status(makeOrderRequest)
          } yield assert(status == BadRequest)
        })
      }

      it("should return BadRequestError if there are not available or non-existing products") {
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
              s2 <- cl.status(addProductsRequest)
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
              s3           <- cl.status(updateRequest)
              makeOrderBody = OrderCreateDto(List(OrderProductDto(id.toString, 10)), "Minsk")
              makeOrderRequest = Request[IO](method = POST, uri = orderAPIAddress)
                .addCookie(clientCookie.name, clientCookie.content)
                .withEntity(makeOrderBody)
              status <- cl.status(makeOrderRequest)
              deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId.toString)
                .addCookie(managerCookie.name, managerCookie.content)
              s4 <- cl.status(deleteGroupRequest)
              deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
                .addCookie(managerCookie.name, managerCookie.content)
              s5 <- cl.status(deleteRequest)
            } yield {
              assert(s1.isSuccess && s2.isSuccess && s3.isSuccess && s4.isSuccess && s5.isSuccess)
              assert(status == BadRequest)
            }
          })
          .unsafeRunSync()
      }

    }

    describe("subscription limitations") {
      it("should return BadRequestError if you try to subscribe supplier/category that already exists") {
        val subscribeCategoryBody = CategorySubscriptionDto(Category.Food)
        val subscribeCategoryRequest =
          Request[IO](method = POST, uri = subscriptionAPIAddress / "category")
            .addCookie(clientCookie.name, clientCookie.content)
            .withEntity(subscribeCategoryBody)
        val subscribeSupplierBody = SupplierSubscriptionDto(1)
        val subscribeSupplierRequest =
          Request[IO](method = POST, uri = subscriptionAPIAddress / "supplier")
            .addCookie(clientCookie.name, clientCookie.content)
            .withEntity(subscribeSupplierBody)

        val removeSupplierSubRequest =
          Request[IO](method = DELETE, uri = subscriptionAPIAddress / "supplier")
            .addCookie(clientCookie.name, clientCookie.content)
            .withEntity(subscribeSupplierBody)
        val removeCategorySubRequest =
          Request[IO](method = DELETE, uri = subscriptionAPIAddress / "category")
            .addCookie(clientCookie.name, clientCookie.content)
            .withEntity(subscribeCategoryBody)
        client
          .use(cl => {
            for {
              s11 <- cl.status(subscribeCategoryRequest)
              s1  <- cl.status(subscribeCategoryRequest)
              s21 <- cl.status(subscribeSupplierRequest)
              s2  <- cl.status(subscribeSupplierRequest)
              s3  <- cl.status(removeSupplierSubRequest)
              s4  <- cl.status(removeCategorySubRequest)
            } yield {
              assert(s11.isSuccess && s21.isSuccess && s3.isSuccess && s4.isSuccess)
              assert(s1 == BadRequest && s2 == BadRequest)
            }
          })
          .unsafeRunSync()
      }

      it("should return BadRequestError if you try to unsubscribe non-existing subscroption") {
        val subscribeSupplierBody = SupplierSubscriptionDto(1)
        val removeSupplierSubRequest =
          Request[IO](method = DELETE, uri = subscriptionAPIAddress / "supplier")
            .addCookie(clientCookie.name, clientCookie.content)
            .withEntity(subscribeSupplierBody)

        client
          .use(cl => {
            for {
              status <- cl.status(removeSupplierSubRequest)
            } yield assert(status == BadRequest)
          })
          .unsafeRunSync()
      }

      it("should return NotFoundError if you try to subscribe non-existing supplier/category") {
        val subscribeSupplierBody = SupplierSubscriptionDto(10)
        val subscribeSupplierRequest =
          Request[IO](method = POST, uri = subscriptionAPIAddress / "supplier")
            .addCookie(clientCookie.name, clientCookie.content)
            .withEntity(subscribeSupplierBody)

        client
          .use(cl => {
            for {
              status <- cl.status(subscribeSupplierRequest)
            } yield assert(status == NotFound)
          })
          .unsafeRunSync()
      }
    }
  }
}
