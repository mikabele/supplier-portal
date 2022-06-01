package error

import error.general.{BadRequestError, NotFoundError}

object attachment {

  object AttachmentError {
    final case class AttachmentNotFound(id: String) extends NotFoundError {
      override def message: String = s"Attachment with id $id doesn't exist"
    }

    final case object AttachmentExists extends BadRequestError {
      override def message: String = s"Attachment for this product already exists"
    }
  }
}
