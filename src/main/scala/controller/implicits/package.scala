package controller

import cats.Show
import service.error.general.GeneralError

package object implicits {
  implicit def catsShowForError[A <: GeneralError]: Show[A] = (t: GeneralError) => t.message
}
