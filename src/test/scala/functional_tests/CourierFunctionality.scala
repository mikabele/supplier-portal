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

class CourierFunctionality extends AnyFunSpec {
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

  describe("Courier") {
    it("should be able to browse orders with status Ordered, assign one of them and then delivered") {
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
            viewOrdersRequest = Request[IO](method = GET, uri = orderAPIAddress / courierUserId)
            actualOrders     <- cl.fetchAs[List[OrderReadDto]](viewOrdersRequest)
            expectedOrders = List(
              OrderReadDto(
                orderId.toString,
                clientUserId,
                List(OrderProductDto(id.toString, 10)),
                OrderStatus.Ordered,
                dateNow,
                200f,
                "Minsk"
              )
            )
            createDeliveryBody = DeliveryCreateDto(orderId.toString)
            createDeliveryRequest = Request[IO](method = POST, uri = deliveryAPIAddress / courierUserId)
              .withEntity(createDeliveryBody)
            deliveryId        <- cl.fetchAs[UUID](createDeliveryRequest)
            deliveredRequest   = Request[IO](method = PUT, uri = deliveryAPIAddress / courierUserId / orderId)
            s6                <- cl.status(deliveredRequest)
            deleteGroupRequest = Request[IO](method = DELETE, uri = groupAPIAddress / groupId)
            s4                <- cl.status(deleteGroupRequest)
            deleteRequest      = Request[IO](method = DELETE, uri = productAPIAddress / id)
            s5                <- cl.status(deleteRequest)
          } yield {
            assert(actualOrders == expectedOrders)
          }
        })
        .unsafeRunSync()
    }

    it("should get NotFound error if try to pick up non-existing order") {
      val createDeliveryBody = DeliveryCreateDto("7befac6d-9e68-4064-927c-b9700438fea1")
      val createDeliveryRequest = Request[IO](method = POST, uri = deliveryAPIAddress / courierUserId)
        .withEntity(createDeliveryBody)

      client
        .use(cl => {
          for {
            status <- cl.status(createDeliveryRequest)
          } yield assert(status == NotFound)
        })
        .unsafeRunSync()
    }
  }
}
