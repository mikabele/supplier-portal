package util

import cats.data.Chain
import cats.syntax.all._
import domain.order.OrderStatus
import service.error.general.GeneralError
import service.error.order.OrderError.InvalidStatusToUpdate
import util.ConvertToErrorsUtil.ErrorsOr

object UpdateOrderStatusRule {
  def checkCurrentStatus(curStatus: OrderStatus, newStatus: OrderStatus): ErrorsOr[Unit] = {
    val wrongRes = Chain[GeneralError](InvalidStatusToUpdate(curStatus, newStatus)).asLeft[Unit]
    curStatus match {
      case OrderStatus.Delivered                                                                          => wrongRes
      case OrderStatus.Assigned if newStatus != OrderStatus.Delivered                                     => wrongRes
      case OrderStatus.Ordered if newStatus != OrderStatus.Assigned && newStatus != OrderStatus.Cancelled => wrongRes
      case _                                                                                              => ().asRight[Chain[GeneralError]]
    }
  }
}
