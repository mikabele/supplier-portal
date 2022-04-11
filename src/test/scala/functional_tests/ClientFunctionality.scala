package functional_tests

import cats.effect.unsafe.implicits._
import cats.effect.{Async, IO, Resource}
import domain.category.Category
import domain.order.OrderStatus
import domain.product.ProductStatus
import dto.attachment.AttachmentReadDto
import dto.criteria.CriteriaDto
import dto.delivery.DeliveryCreateDto
import dto.group.{GroupCreateDto, GroupWithProductsDto, GroupWithUsersDto}
import dto.order.{OrderCreateDto, OrderProductDto, OrderReadDto}
import dto.product.{ProductCreateDto, ProductReadDto, ProductUpdateDto}
import dto.subscription.{CategorySubscriptionDto, SupplierSubscriptionDto}
import dto.supplier.SupplierDto
import io.circe.generic.auto._
import org.http4s.Request
import org.http4s.blaze.client._
import org.http4s.circe.CirceEntityCodec.{circeEntityDecoder, circeEntityEncoder}
import org.http4s.client.Client
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.scalatest.funspec.AnyFunSpec

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

// TODO - rewrite tests with authentication

class ClientFunctionality extends AnyFunSpec {
  def clientResource[F[_]: Async]: Resource[F, Client[F]] = {
    import scala.concurrent.ExecutionContext.Implicits.global
    BlazeClientBuilder[F].withExecutionContext(global).resource
  }

  val client: Resource[IO, Client[IO]] = clientResource[IO]

  private val productAPIAddress      = uri"http://localhost:8088/api/product"
  private val groupAPIAddress        = uri"http://localhost:8088/api/product_group"
  private val orderAPIAddress        = uri"http://localhost:8088/api/order"
  private val subscriptionAPIAddress = uri"http://localhost:8088/api/subscription"
  private val deliveryAPIAddress     = uri"http://localhost:8088/api/delivery"

  private val managerUserId = "e188ba42-501a-4841-a3d3-5a4bcfcaeb33"
  private val courierUserId = "34418133-92b6-46b1-bb3b-edf2a9af1413"
  private val clientUserId  = "aa32d3e1-526e-4d76-b545-cf7cd413af6d"
  private val supplierDto   = SupplierDto(1, "Mem", "Minsk")
  private val dtf           = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  private val dateNow       = dtf.format(LocalDateTime.now())

  describe("Client") {
    it(
      "should be able browse and search products by different criterias. He can see only Available and NotAvailable Products. User should be in at least one group where product is. Otherwise, he can't see product"
    ) {
      val createProductBody = ProductCreateDto("testproduct", Category.Food, 1, 20f, None)
      val request =
        Request[IO](method = POST, uri = productAPIAddress).withEntity(createProductBody)
      val createGroupBody    = GroupCreateDto("age18")
      val createGroupRequest = Request[IO](method = POST, uri = groupAPIAddress).withEntity(createGroupBody)
      val criteria           = CriteriaDto(minPrice = Some(10f))
      val criteriaRequest =
        Request[IO](method = POST, uri = productAPIAddress / "search" / clientUserId).withEntity(criteria)
      client
        .use(cl => {
          for {
            id              <- cl.fetchAs[UUID](request)
            groupId         <- cl.fetchAs[UUID](createGroupRequest)
            groupWithUser    = GroupWithUsersDto(groupId.toString, List(clientUserId))
            groupWithProduct = GroupWithProductsDto(groupId.toString, List(id.toString))
            addUsersRequest  = Request[IO](method = POST, uri = groupAPIAddress / "users").withEntity(groupWithUser)
            s1              <- cl.status(addUsersRequest)
            addProductsRequest = Request[IO](method = POST, uri = groupAPIAddress / "products")
              .withEntity(groupWithProduct)
            s2             <- cl.status(addProductsRequest)
            getRequest      = Request[IO](method = GET, uri = productAPIAddress / clientUserId)
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
            deleteGroupRequest        = Request[IO](method = DELETE, uri = groupAPIAddress / groupId)
            s3                       <- cl.status(deleteGroupRequest)
            deleteRequest             = Request[IO](method = DELETE, uri = productAPIAddress / id)
            _                        <- cl.status(deleteRequest)
          } yield {
            assert(actualProducts == expectedProducts)
            assert(actualProductsByCriteria == expectedProducts)
            assert(s1.isSuccess && s2.isSuccess && s3.isSuccess)
          }
        })
        .unsafeRunSync()
    }

    it(
      "should be able to make order, view his orders, cancel his order. Can add only available products in orderDto"
    ) {
      val createProductBody = ProductCreateDto("testproduct", Category.Food, 1, 20f, None)
      val request =
        Request[IO](method = POST, uri = productAPIAddress).withEntity(createProductBody)
      val createGroupBody    = GroupCreateDto("age18")
      val createGroupRequest = Request[IO](method = POST, uri = groupAPIAddress).withEntity(createGroupBody)
      client
        .use(cl => {
          for {
            id              <- cl.fetchAs[UUID](request)
            groupId         <- cl.fetchAs[UUID](createGroupRequest)
            groupWithUser    = GroupWithUsersDto(groupId.toString, List(clientUserId))
            groupWithProduct = GroupWithProductsDto(groupId.toString, List(id.toString))
            addUsersRequest  = Request[IO](method = POST, uri = groupAPIAddress / "users").withEntity(groupWithUser)
            s1              <- cl.status(addUsersRequest)
            addProductsRequest = Request[IO](method = POST, uri = groupAPIAddress / "products")
              .withEntity(groupWithProduct)
            s2           <- cl.status(addProductsRequest)
            makeOrderBody = OrderCreateDto(List(OrderProductDto(id.toString, 10)), "Minsk")
            makeOrderRequest = Request[IO](method = POST, uri = orderAPIAddress / clientUserId)
              .withEntity(makeOrderBody)
            orderId          <- cl.fetchAs[UUID](makeOrderRequest)
            viewOrdersRequest = Request[IO](method = GET, uri = orderAPIAddress / clientUserId)
            status           <- cl.status(viewOrdersRequest)

            cancelOrderRequest = Request[IO](method = PUT, uri = orderAPIAddress / clientUserId / orderId)
            s3                <- cl.status(cancelOrderRequest)
            deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId)
            s4                <- cl.status(deleteGroupRequest)
            deleteRequest      = Request[IO](method = DELETE, uri = productAPIAddress / id)
            s5                <- cl.status(deleteRequest)
          } yield {
            assert(status.isSuccess && s1.isSuccess && s2.isSuccess && s3.isSuccess && s4.isSuccess && s5.isSuccess)
          }
        })
        .unsafeRunSync()
    }

    it("should be able to subscribe category/supplier , view his subscriptions, remove subscription") {
      val subscribeCategoryBody = CategorySubscriptionDto(Category.Food)
      val subscribeCategoryRequest =
        Request[IO](method = POST, uri = subscriptionAPIAddress / "category" / clientUserId)
          .withEntity(subscribeCategoryBody)
      val subscribeSupplierBody = SupplierSubscriptionDto(1)
      val subscribeSupplierRequest =
        Request[IO](method = POST, uri = subscriptionAPIAddress / "supplier" / clientUserId)
          .withEntity(subscribeSupplierBody)

      val getSupplierSubRequest = Request[IO](method = GET, uri = subscriptionAPIAddress / "supplier" / clientUserId)
      val getCategorySubRequest = Request[IO](method = GET, uri = subscriptionAPIAddress / "category" / clientUserId)
      val removeSupplierSubRequest =
        Request[IO](method = DELETE, uri = subscriptionAPIAddress / "supplier" / clientUserId)
          .withEntity(subscribeSupplierBody)
      val removeCategorySubRequest =
        Request[IO](method = DELETE, uri = subscriptionAPIAddress / "category" / clientUserId)
          .withEntity(subscribeCategoryBody)
      client
        .use(cl => {
          for {
            s1                <- cl.status(subscribeCategoryRequest)
            s2                <- cl.status(subscribeSupplierRequest)
            actualCategories  <- cl.fetchAs[List[Category]](getCategorySubRequest)
            expectedCategories = List(Category.Food)
            actualSuppliers   <- cl.fetchAs[List[SupplierDto]](getSupplierSubRequest)
            expectedSuppliers  = List(supplierDto)
            s3                <- cl.status(removeSupplierSubRequest)
            s4                <- cl.status(removeCategorySubRequest)
          } yield {
            assert(actualSuppliers == expectedSuppliers)
            assert(actualCategories == expectedCategories)
            assert(s1.isSuccess && s2.isSuccess && s3.isSuccess && s4.isSuccess)
          }
        })
        .unsafeRunSync()
    }

    it("should return NotFoundError if you try to cancel non-existing order") {
      val cancelOrderRequest =
        Request[IO](method = PUT, uri = orderAPIAddress / clientUserId / "7befac6d-9e68-4064-927c-b9700438fea1")
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
        Request[IO](method = POST, uri = productAPIAddress).withEntity(createProductBody)
      val createGroupBody    = GroupCreateDto("age18")
      val createGroupRequest = Request[IO](method = POST, uri = groupAPIAddress).withEntity(createGroupBody)
      client
        .use(cl => {
          for {
            id              <- cl.fetchAs[UUID](request)
            groupId         <- cl.fetchAs[UUID](createGroupRequest)
            groupWithUser    = GroupWithUsersDto(groupId.toString, List(clientUserId))
            groupWithProduct = GroupWithProductsDto(groupId.toString, List(id.toString))
            addUsersRequest  = Request[IO](method = POST, uri = groupAPIAddress / "users").withEntity(groupWithUser)
            s1              <- cl.status(addUsersRequest)
            addProductsRequest = Request[IO](method = POST, uri = groupAPIAddress / "products")
              .withEntity(groupWithProduct)
            s2           <- cl.status(addProductsRequest)
            makeOrderBody = OrderCreateDto(List(OrderProductDto(id.toString, 10)), "Minsk")
            makeOrderRequest = Request[IO](method = POST, uri = orderAPIAddress / clientUserId)
              .withEntity(makeOrderBody)
            orderId           <- cl.fetchAs[UUID](makeOrderRequest)
            createDeliveryBody = DeliveryCreateDto(orderId.toString)
            createDeliveryRequest = Request[IO](method = POST, uri = deliveryAPIAddress / courierUserId)
              .withEntity(createDeliveryBody)
            _                 <- cl.status(createDeliveryRequest)
            cancelOrderRequest = Request[IO](method = PUT, uri = orderAPIAddress / clientUserId / orderId)
            status            <- cl.status(cancelOrderRequest)
            deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId)
            s4                <- cl.status(deleteGroupRequest)
            deleteRequest      = Request[IO](method = DELETE, uri = productAPIAddress / id)
            s5                <- cl.status(deleteRequest)
          } yield assert(status == BadRequest)
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
      val makeOrderRequest = Request[IO](method = POST, uri = orderAPIAddress / clientUserId)
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
        Request[IO](method = POST, uri = productAPIAddress).withEntity(createProductBody)
      val createGroupBody    = GroupCreateDto("age18")
      val createGroupRequest = Request[IO](method = POST, uri = groupAPIAddress).withEntity(createGroupBody)
      client
        .use(cl => {
          for {
            id              <- cl.fetchAs[UUID](request)
            groupId         <- cl.fetchAs[UUID](createGroupRequest)
            groupWithUser    = GroupWithUsersDto(groupId.toString, List(clientUserId))
            groupWithProduct = GroupWithProductsDto(groupId.toString, List(id.toString))
            addUsersRequest  = Request[IO](method = POST, uri = groupAPIAddress / "users").withEntity(groupWithUser)
            s1              <- cl.status(addUsersRequest)
            addProductsRequest = Request[IO](method = POST, uri = groupAPIAddress / "products")
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
            updateRequest = Request[IO](method = PUT, uri = productAPIAddress).withEntity(updateProductBody)
            _            <- cl.status(updateRequest)
            makeOrderBody = OrderCreateDto(List(OrderProductDto(id.toString, 10)), "Minsk")
            makeOrderRequest = Request[IO](method = POST, uri = orderAPIAddress / clientUserId)
              .withEntity(makeOrderBody)
            status            <- cl.status(makeOrderRequest)
            deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId)
            s4                <- cl.status(deleteGroupRequest)
            deleteRequest      = Request[IO](method = DELETE, uri = productAPIAddress / id)
            s5                <- cl.status(deleteRequest)
          } yield {
            assert(status == BadRequest)
          }
        })
        .unsafeRunSync()
    }

    it("should return BadRequestError if you try to subscribe supplier/category that already exists") {
      val subscribeCategoryBody = CategorySubscriptionDto(Category.Food)
      val subscribeCategoryRequest =
        Request[IO](method = POST, uri = subscriptionAPIAddress / "category" / clientUserId)
          .withEntity(subscribeCategoryBody)
      val subscribeSupplierBody = SupplierSubscriptionDto(1)
      val subscribeSupplierRequest =
        Request[IO](method = POST, uri = subscriptionAPIAddress / "supplier" / clientUserId)
          .withEntity(subscribeSupplierBody)

      val removeSupplierSubRequest =
        Request[IO](method = DELETE, uri = subscriptionAPIAddress / "supplier" / clientUserId)
          .withEntity(subscribeSupplierBody)
      val removeCategorySubRequest =
        Request[IO](method = DELETE, uri = subscriptionAPIAddress / "category" / clientUserId)
          .withEntity(subscribeCategoryBody)
      client
        .use(cl => {
          for {
            _  <- cl.status(subscribeCategoryRequest)
            s1 <- cl.status(subscribeCategoryRequest)
            _  <- cl.status(subscribeSupplierRequest)
            s2 <- cl.status(subscribeSupplierRequest)
            s3 <- cl.status(removeSupplierSubRequest)
            s4 <- cl.status(removeCategorySubRequest)
          } yield {
            assert(s1 == BadRequest && s2 == BadRequest)
          }
        })
        .unsafeRunSync()
    }

    it("should return BadRequestError if you try to unsubscribe non-existing subscroption") {
      val subscribeSupplierBody = SupplierSubscriptionDto(1)
      val removeSupplierSubRequest =
        Request[IO](method = DELETE, uri = subscriptionAPIAddress / "supplier" / clientUserId)
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
        Request[IO](method = POST, uri = subscriptionAPIAddress / "supplier" / clientUserId)
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
