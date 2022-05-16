package functional_tests

import cats.effect.{Async, ConcurrentEffect, ContextShift, IO, Resource, Timer}
import domain.order.OrderStatus
import dto.delivery.DeliveryCreateDto
import dto.group.{GroupCreateDto, GroupWithProductsDto, GroupWithUsersDto}
import dto.order.{OrderCreateDto, OrderProductDto, OrderReadDto}
import dto.product.ProductCreateDto
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

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

class CourierFunctionality extends AnyFunSpec {
  def clientResource[F[_]: Async: ConcurrentEffect]: Resource[F, Client[F]] = {
    BlazeClientBuilder[F](global).resource
  }

  implicit val cs:    ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO]        = IO.timer(global)

  val client: Resource[IO, Client[IO]] = clientResource[IO]

  private val productAPIAddress  = uri"http://localhost:8088/api/product"
  private val groupAPIAddress    = uri"http://localhost:8088/api/product_group"
  private val authAPIAddress     = uri"http://localhost:8088/api/auth"
  private val orderAPIAddress    = uri"http://localhost:8088/api/order"
  private val deliveryAPIAddress = uri"http://localhost:8088/api/delivery"

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

  describe("Courier") {
    it("should be able to browse orders with status Ordered, assign one of them and then delivered") {
      val createProductBody = ProductCreateDto("testproduct", 1, 1, 20f, None)
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
              .addCookie(courierCookie.name, courierCookie.content)
            actualOrders <- cl.fetchAs[List[OrderReadDto]](viewOrdersRequest)
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
            createDeliveryRequest = Request[IO](method = POST, uri = deliveryAPIAddress)
              .addCookie(courierCookie.name, courierCookie.content)
              .withEntity(createDeliveryBody)
            s7 <- cl.status(createDeliveryRequest)
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
            assert(s1.isSuccess && s2.isSuccess && s4.isSuccess && s5.isSuccess && s6.isSuccess && s7.isSuccess)
            assert(actualOrders.map(_.copy(orderedStartDate = dateNow)) == expectedOrders)
          }
        })
        .unsafeRunSync()
    }

    it("should get NotFound error if try to pick up non-existing order") {
      val createDeliveryBody = DeliveryCreateDto("7befac6d-9e68-4064-927c-b9700438fea1")
      val createDeliveryRequest = Request[IO](method = POST, uri = deliveryAPIAddress)
        .addCookie(courierCookie.name, courierCookie.content)
        .withEntity(createDeliveryBody)

      client
        .use(cl => {
          for {
            status <- cl.status(createDeliveryRequest)
          } yield assert(status == NotFound)
        })
        .unsafeRunSync()
    }

    it("Will throw 400 Error when pick-up order if order has status differed from Ordered") {
      val createProductBody = ProductCreateDto("testproduct", 1, 1, 20f, None)
      val request =
        Request[IO](method = POST, uri = productAPIAddress)
          .withEntity(createProductBody)
          .addCookie(managerCookie.name, managerCookie.content)

      client
        .use(cl => {
          for {
            id           <- cl.fetchAs[UUID](request)
            makeOrderBody = OrderCreateDto(List(OrderProductDto(id.toString, 10)), "Minsk")
            makeOrderRequest = Request[IO](method = POST, uri = orderAPIAddress)
              .addCookie(clientCookie.name, clientCookie.content)
              .withEntity(makeOrderBody)
            orderId           <- cl.fetchAs[UUID](makeOrderRequest)
            createDeliveryBody = DeliveryCreateDto(orderId.toString)
            createDeliveryRequest = Request[IO](method = POST, uri = deliveryAPIAddress)
              .addCookie(courierCookie.name, courierCookie.content)
              .withEntity(createDeliveryBody)
            s7     <- cl.status(createDeliveryRequest)
            actual <- cl.status(createDeliveryRequest)
            deliveredRequest = Request[IO](method = PUT, uri = deliveryAPIAddress / orderId.toString)
              .addCookie(courierCookie.name, courierCookie.content)
            s6 <- cl.status(deliveredRequest)
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
              .addCookie(managerCookie.name, managerCookie.content)
            s5 <- cl.status(deleteRequest)
          } yield {
            assert(actual == BadRequest && s7.isSuccess && s6.isSuccess && s5.isSuccess)
          }
        })
        .unsafeRunSync()
    }

    it("delivered: Will throw 404 Error if order doesn’t exist") {

      val deliveredRequest =
        Request[IO](method = PUT, uri = deliveryAPIAddress / "7befac6d-9e68-4064-927c-b9700438fea1")
          .addCookie(courierCookie.name, courierCookie.content)

      client
        .use(cl => {
          for {
            status <- cl.status(deliveredRequest)
          } yield assert(status == NotFound)
        })
        .unsafeRunSync()
    }

    it("delivered: Will throw 400 Error if order has status differed from PickedUp") {
      val createProductBody = ProductCreateDto("testproduct", 1, 1, 20f, None)
      val request =
        Request[IO](method = POST, uri = productAPIAddress)
          .withEntity(createProductBody)
          .addCookie(managerCookie.name, managerCookie.content)

      client
        .use(cl => {
          for {
            id           <- cl.fetchAs[UUID](request)
            makeOrderBody = OrderCreateDto(List(OrderProductDto(id.toString, 10)), "Minsk")
            makeOrderRequest = Request[IO](method = POST, uri = orderAPIAddress)
              .addCookie(clientCookie.name, clientCookie.content)
              .withEntity(makeOrderBody)
            orderId <- cl.fetchAs[UUID](makeOrderRequest)

            deliveredRequest = Request[IO](method = PUT, uri = deliveryAPIAddress / orderId.toString)
              .addCookie(courierCookie.name, courierCookie.content)
            s6 <- cl.status(deliveredRequest)
            cancelOrderRequest = Request[IO](method = PUT, uri = orderAPIAddress / orderId.toString)
              .addCookie(clientCookie.name, clientCookie.content)
            s3 <- cl.status(cancelOrderRequest)
            deleteRequest = Request[IO](method = DELETE, uri = productAPIAddress / id.toString)
              .addCookie(managerCookie.name, managerCookie.content)
            s5 <- cl.status(deleteRequest)
          } yield {
            assert(s6 == BadRequest && s3.isSuccess && s5.isSuccess)
          }
        })
        .unsafeRunSync()
    }
  }
}
