package domain

import enumeratum.EnumEntry.Snakecase
import enumeratum.values.{IntCirceEnum, IntDoobieEnum, IntEnum, IntEnumEntry}
import io.circe.Encoder

object category {
  // TODO : add more categories

  sealed abstract class Category(val value: Int) extends IntEnumEntry with Snakecase
  case object Category extends IntEnum[Category] with IntCirceEnum[Category] with IntDoobieEnum[Category] {

    final case object Food extends Category(1)
    final case object Electronics extends Category(2)

    val values: IndexedSeq[Category] = findValues

//    implicit override val circeEncoder: Encoder[Category] =
//      Encoder.forProduct2("id", "name")(s => (s.value, s.toString))
  }
}
