package repository

import cats.effect.Sync
import domain.order.{OrderCreateDomain, OrderReadDomain}
import domain.user.ReadAuthorizedUser
import doobie.util.transactor.Transactor
import repository.impl.DoobieOrderRepositoryImpl

import java.util.UUID

trait OrderRepository[F[_]] {
  def getById(id: UUID): F[Option[OrderReadDomain]]

  def cancelOrder(id: UUID): F[Int]

  def viewActiveOrders(user: ReadAuthorizedUser): F[List[OrderReadDomain]]

  def createOrder(user: ReadAuthorizedUser, domain: OrderCreateDomain): F[UUID]

  def checkActiveOrderWithProduct(productId: UUID): F[Int]
}

object OrderRepository {
  def of[F[_]: Sync](tx: Transactor[F]): OrderRepository[F] = {
    new DoobieOrderRepositoryImpl(tx)
  }
}
