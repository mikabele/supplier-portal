package domain

object category {
  // TODO : add more categories

  sealed abstract class Category private (val id: Int)
  object Category {
    final case object Food extends Category(0)
    final case object Electronics extends Category(1)
  }
}
