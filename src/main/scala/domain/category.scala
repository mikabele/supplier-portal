package domain

import doobie.Read
import enumeratum.values.{IntCirceEnum, IntDoobieEnum, IntEnum, IntEnumEntry}
import eu.timepit.refined.refineV
import types.PositiveInt

object category {
  // TODO : add more categories

  sealed abstract class Category(val value: Int) extends IntEnumEntry
  case object Category extends IntEnum[Category] with IntCirceEnum[Category] with IntDoobieEnum[Category] {

    final case object Food extends Category(1)
    final case object Electronics extends Category(2)

    val values: IndexedSeq[Category] = findValues
  }
}
