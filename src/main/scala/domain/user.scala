package domain

import doobie.postgres.implicits._
import enumeratum.EnumEntry.Snakecase
import enumeratum.{CirceEnum, DoobieEnum, EnumEntry, _}
import io.circe.Json
import io.circe.syntax._
import types._

// TODO - when will work on it don't forget about password encryption(encoding)

object user {

  sealed trait Role extends EnumEntry with Snakecase

  case object Role extends Enum[Role] with CirceEnum[Role] with DoobieEnum[Role] {

    val values: IndexedSeq[Role] = findValues

    final case object Manager extends Role
    final case object Client extends Role
    final case object Courier extends Role

    Role.values.foreach { role => assert(role.asJson == Json.fromString(role.entryName)) }

    implicit override lazy val enumMeta: doobie.Meta[Role] =
      pgEnumString("user_role", Role.withName, _.entryName)
  }

  final case class AuthorizedUser(
    userID:   Int,
    login:    NonEmptyStr,
    password: NonEmptyStr,
    role:     Role,
    phone:    PhoneStr,
    email:    EmailStr
  )

  final case class ReadAuthorizedUser(
    id:      UuidStr,
    name:    NonEmptyStr,
    surname: NonEmptyStr,
    role:    Role,
    phone:   PhoneStr,
    email:   EmailStr
  )
}
