package service.error

import service.error.general.{GeneralError, NotFoundError}

import java.util.UUID

object attachment {
  trait AttachmentError extends GeneralError

  object AttachmentError {
    final case class AttachmentNotFound(id: String) extends AttachmentError with NotFoundError {
      override def message: String = s"Attachment with id $id doesn't exist"
    }
  }
}
