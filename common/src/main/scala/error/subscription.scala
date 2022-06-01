package error

import error.general.BadRequestError

object subscription {

  object SubscriptionError {
    final case object SubscriptionExists extends BadRequestError {
      override def message: String = s"Subscription already exists"
    }

    final case object SubscriptionNotExists extends BadRequestError {
      override def message: String = s"You can't remove subscription - subscription doesn't exist"
    }
  }
}
