package dto

import domain.order.OrderStatus

object order {
  final case class OrderDto(
    id:         String,
    orderItems: List[OrderItemDto],
    status:     OrderStatus
  )

  final case class OrderItemDto(
    productId: String,
    count:     Int
  )
}
