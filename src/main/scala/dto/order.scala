package dto

import domain.order.OrderStatus

object order {
  final case class OrderCreateDto(
    orderItems: List[OrderProductDto],
    address:    String
  )

  final case class OrderProductDto(
    productId: String,
    count:     Int
  )

  final case class OrderUpdateDto(
    id:          String,
    orderStatus: OrderStatus
  )

  final case class OrderReadDto(
    id:               String,
    userId:           String,
    orderItems:       List[OrderProductDto],
    status:           OrderStatus,
    orderedStartDate: String,
    total:            Float,
    address:          String
  )
}
