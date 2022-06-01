package domain

import types.{NonEmptyStr, PositiveInt}

object category {
  // TODO : add more categories
  // TODO - make it as case class not enum to easy support adding new categories

//  sealed abstract class Category(val value: Int) extends IntEnumEntry with Snakecase
//  case object Category extends IntEnum[Category] with IntCirceEnum[Category] with IntDoobieEnum[Category] {
//
//    final case object Food extends Category(1)
//    final case object Electronics extends Category(2)
//
//    val values: IndexedSeq[Category] = findValues
//  }

  final case class CategoryDomain(
    id:   PositiveInt,
    name: NonEmptyStr
  )
}
