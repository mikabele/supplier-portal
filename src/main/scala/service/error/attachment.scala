package service.error

import service.error.general.{GeneralError, NotFoundError}

import java.util.UUID

object attachment {

  object AttachmentError {
    final case class AttachmentNotFound(id: String) extends NotFoundError {
      override def message: String = s"Attachment with id $id doesn't exist"
    }
  }
}
