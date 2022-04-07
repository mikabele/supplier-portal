package dto

import domain.order.OrderStatus

object order {
  final case class CreateOrderDto(
    userId:     String, // temp field while i don't implement authorization logic
    orderItems: List[OrderItemDto],
    address:    String
  )

  final case class OrderItemDto(
    productId: String,
    count:     Int
  )

  final case class UpdateOrderDto(
    id:          String,
    orderStatus: OrderStatus
  )

  final case class ReadOrderDto(
    id:               String,
    userId:           String,
    orderItems:       List[OrderItemDto],
    status:           OrderStatus,
    orderedStartDate: String,
    total:            Float,
    address:          String
  )
}
