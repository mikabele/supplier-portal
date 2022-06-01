package repository

import cats.effect.Async
import domain.group.{GroupCreateDomain, GroupReadDomain, GroupWithProductsDomain, GroupWithUsersDomain}
import doobie.util.transactor.Transactor
import repository.impl.DoobieProductGroupRepositoryImpl

import java.util.UUID

trait ProductGroupRepository[F[_]] {
  def addProducts(domain: GroupWithProductsDomain): F[Int]

  def addUsers(domain: GroupWithUsersDomain): F[Int]

  def removeProducts(domain: GroupWithProductsDomain): F[Int]

  def removeUsers(domain: GroupWithUsersDomain): F[Int]

  def addGroup(domain: GroupCreateDomain): F[UUID]

  def deleteGroup(id: UUID): F[Int]

  def showGroups(): F[List[GroupReadDomain]]

  def getById(id: UUID): F[Option[GroupReadDomain]]

  def checkByName(name: String): F[Option[Int]] //technical method
}

object ProductGroupRepository {
  def of[F[_]: Async](tx: Transactor[F]): ProductGroupRepository[F] = {
    new DoobieProductGroupRepositoryImpl[F](tx)
  }
}
