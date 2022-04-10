package service.error

import service.error.general.BadRequestError

object delivery {
  object DeliveryError {
    final case object InvalidDeliveryCourier extends BadRequestError {
      override def message: String = s"You are not the courier of order!"
    }
  }
}
