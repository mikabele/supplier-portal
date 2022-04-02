package domain

import doobie.Read
import eu.timepit.refined.refineV
import types.PositiveInt

object category {
  // TODO : add more categories

  sealed abstract class Category(val id: Int)
  object Category {
    final case object Food extends Category(1)
    final case object Electronics extends Category(2)

    def of(id: Int): Category = id match {
      case 1 => Food
      case 2 => Electronics
    }
  }
}
