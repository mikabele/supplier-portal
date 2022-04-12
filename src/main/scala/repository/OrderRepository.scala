package repository

import cats.effect.Sync
import domain.order.{OrderCreateDomain, OrderReadDomain}
import doobie.util.transactor.Transactor
import repository.impl.DoobieOrderRepositoryImpl

import java.util.UUID

trait OrderRepository[F[_]] {
  def getById(id: UUID): F[Option[OrderReadDomain]]

  def cancelOrder(id: UUID): F[Int]

  def viewActiveOrders(userId: UUID): F[List[OrderReadDomain]]

  def createOrder(userId: UUID, domain: OrderCreateDomain): F[UUID]

  def checkActiveOrderWithProduct(productId: UUID): F[Int]
}

object OrderRepository {
  def of[F[_]: Sync](tx: Transactor[F]): OrderRepository[F] = {
    new DoobieOrderRepositoryImpl(tx)
  }
}
