package repository.impl

import cats.data.Validated.Valid
import doobie.{Read, Write}
import types._
import doobie.refined.implicits._ // never delete this row
import domain.category.Category
import domain.order.{OrderStatus, ReadOrder, ReadOrderItem}
import domain.product.{ProductStatus, ReadProduct}
import domain.supplier.Supplier
import doobie.postgres._
import doobie.postgres.circe._
import doobie.postgres.implicits._
import dto.attachment.ReadAttachmentDto
import dto.order.OrderItemDto
import util.ModelMapper._

import java.util.UUID

// TODO - remove it will empty

package object implicits {

//  implicit def readProductRead: Read[ReadProduct] =
//    Read[
//      (
//        UuidStr,
//        NonEmptyStr,
//        Category,
//        Supplier,
//        NonNegativeFloat,
//        String,
//        ProductStatus,
//        DateStr,
//        List[Option[UUID]],
//        List[Option[String]]
//      )
//    ]
//      .map {
//        case (
//              product_id,
//              product_name,
//              category,
//              supplier,
//              price,
//              description,
//              status,
//              publicationPeriod,
//              attachmentIds,
//              attachmentUrls
//            ) =>
//          val attachments = attachmentIds
//            .zip(attachmentUrls)
//            .filter(_ != (None, None))
//            .map(a => {
//              val (id, url) = a
//              val dto       = ReadAttachmentDto(id.map(_.toString).getOrElse(""), url.getOrElse(""))
//              validateReadAttachmentDto(dto) match {
//                case Valid(a) => a
//              }
//            })
//          ReadProduct(
//            product_id,
//            product_name,
//            category,
//            supplier,
//            price,
//            description,
//            status,
//            publicationPeriod,
//            attachments
//          )
//      }

//  implicit def readOrderRead: Read[ReadOrder] = {
//    Read[(UuidStr, UuidStr, List[UUID], List[Int], OrderStatus, DateStr, NonNegativeFloat, NonEmptyStr)].map {
//      case (order_id, user_id, order_item_ids, order_item_counts, orderStatus, orderedStartDate, total, address) =>
//        val orderItems = order_item_ids
//          .zip(order_item_counts)
//          .map(t => {
//            val (i, c) = t
//            val dto    = OrderItemDto(i.toString, c)
//            validateOrderItemDto(dto) match {
//              case Valid(a) => a
//            }
//          })
//        ReadOrder(order_id, user_id, orderItems, orderStatus, orderedStartDate, total, address)
//    }
//  }
//
//  implicit val writeOrderItem: Write[OrderItem] =
//    Write[(String, Int)].contramap(oi => (oi.productId.value, oi.count.value))

//  implicit def supplierRead: Read[Supplier] = Read[(PositiveInt, NonEmptyStr, NonEmptyStr)]
//    .map { case (id, name, address) =>
//      Supplier(id, name, address)
//    }
}
