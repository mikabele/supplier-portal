package service.error

import service.error.general.{BadRequestError, NotFoundError}

object group {
  object ProductGroupError {
    final case class ProductGroupNotFound(id: String) extends NotFoundError {
      override def message: String = s"Group with id $id doesn't exist"
    }

    final case class UserIsNotInGroup(userId: String, groupId: String) extends BadRequestError {
      override def message: String = s"User with id $userId is not in the group with id $groupId"
    }

    final case class ProductIsNotInGroup(productId: String, groupId: String) extends BadRequestError {
      override def message: String = s"Product with id $productId is not in the group with id $groupId"
    }

    final case object EmptyGroup extends BadRequestError {
      override def message: String = s"Group DTO should have at least one element to insert/delete"
    }

    final case class DuplicatedUserInGroup(id: String) extends BadRequestError {
      override def message: String = s"User with $id was found in the group more than once"
    }

    final case class DuplicatedProductInGroup(id: String) extends BadRequestError {
      override def message: String = s"Product with $id was found in the group more than once"
    }

    final case class UserAlreadyInGroup(id: String) extends BadRequestError {
      override def message: String = s"User with $id is already in the group"
    }

    final case class ProductAlreadyInGroup(id: String) extends BadRequestError {
      override def message: String = s"Product with $id is already in the group"
    }

    final case object GroupExists extends BadRequestError {
      override def message: String = s"Product group already exists"
    }
  }
}
