package service.error

import service.error.general.{BadRequestError, GeneralError}

object subscription {
  trait SubscriptionError extends GeneralError

  object SubscriptionError {
    final case object SubscriptionExists extends SubscriptionError with BadRequestError {
      override def message: String = s"Subscription already exists"
    }
  }
}
