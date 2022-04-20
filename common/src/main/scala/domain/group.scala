package domain

import cats.data.NonEmptyList
import types._

object group {
  final case class GroupCreateDomain(
    name: NonEmptyStr
  )

  final case class GroupWithUsersDomain(
    id:      UuidStr,
    userIds: NonEmptyList[UuidStr]
  )

  final case class GroupWithProductsDomain(
    id:         UuidStr,
    productIds: NonEmptyList[UuidStr]
  )

  final case class GroupReadDomain(
    id:         UuidStr,
    name:       NonEmptyStr,
    userIds:    List[UuidStr],
    productIds: List[UuidStr]
  )

  final case class GroupReadDbDomain(
    id:   UuidStr,
    name: NonEmptyStr
  )

  final case class GroupUserDomain(
    userId:  UuidStr,
    groupId: UuidStr
  )

  final case class GroupProductDomain(
    productId: UuidStr,
    groupId:   UuidStr
  )

}
