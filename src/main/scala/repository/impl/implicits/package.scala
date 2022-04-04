package repository.impl

import cats.data.Validated.Valid
import doobie.{Read, Write}
import types._
import util.CaseConversionUtil._
import doobie.refined.implicits._
import domain.category.Category
import domain.order.{OrderItem, OrderStatus, ReadOrder}
import domain.product.{ProductStatus, ReadProduct}
import domain.supplier.Supplier
import doobie.postgres._
import doobie.postgres.implicits._
import dto.order.OrderItemDto
import util.ModelMapper._

import java.util.UUID

// TODO - read how to use Refined inside Lists to read data

package object implicits {
  implicit val readProductStatus:  Read[ProductStatus]  = Read[String].map(v => ProductStatus.of(snakeToCamel(v)))
  implicit val writeProductStatus: Write[ProductStatus] = Write[String].contramap(v => camelToSnake(v.toString))

  implicit val readOrderStatus:  Read[OrderStatus]  = Read[String].map(v => OrderStatus.of(snakeToCamel(v)))
  implicit val writeOrderStatus: Write[OrderStatus] = Write[String].contramap(v => camelToSnake(v.toString))

  implicit val readCategory:  Read[Category]  = Read[Int].map(id => Category.of(id))
  implicit val writeCategory: Write[Category] = Write[Int].contramap(c => c.id)

  implicit def readProductRead: Read[ReadProduct] =
    Read[(UuidStr, NonEmptyStr, Category, Supplier, NonNegativeFloat, String, ProductStatus)]
      .map { case (product_id, product_name, category, supplier, price, description, status) =>
        ReadProduct(product_id, product_name, category, supplier, price, description, status)
      }

  implicit def readOrderRead: Read[ReadOrder] =
    Read[(UuidStr, List[UUID], List[Int], OrderStatus, DateStr, NonNegativeFloat)].map {
      case (order_id, order_item_ids, order_item_counts, orderStatus, orderedStartDate, total) =>
        val orderItems = order_item_ids
          .zip(order_item_counts)
          .map(t => {
            val (i, c) = t
            val dto    = OrderItemDto(i.toString, c)
            validateOrderItemDto(dto) match {
              case Valid(a) => a
            }
          })
        ReadOrder(order_id, orderItems, orderStatus, orderedStartDate, total)
    }

  implicit val writeOrderItem: Write[OrderItem] =
    Write[(String, Int)].contramap(oi => (oi.productId.value, oi.count.value))
}
