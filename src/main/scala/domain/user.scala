package domain

import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.string._

object user {
  type NonEmptyStr = String Refined MatchesRegex["\\w+"] And Trimmed

  sealed trait Role
  object Role {
    final case object Manager extends Role
    final case object Client extends Role
    final case object Courier extends Role
  }

  final case class User(userID: Int, name: NonEmptyStr, role: Role)
}
