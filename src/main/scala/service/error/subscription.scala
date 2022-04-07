package service.error

import service.error.general.{BadRequestError, GeneralError}

object subscription {
  trait SubscriptionError extends GeneralError

  object SubscriptionError {
    final case object SubscriptionExists extends SubscriptionError with BadRequestError {
      override def message: String = s"Subscription already exists"
    }

    final case object SubscriptionNotExists extends SubscriptionError with BadRequestError {
      override def message: String = s"You can't remove subscription - subscription doesn't exist"
    }
  }
}
