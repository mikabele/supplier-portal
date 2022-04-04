package repository

import cats.effect.Sync
import domain.order.{CreateOrder, ReadOrder, UpdateOrder}
import doobie.util.transactor.Transactor
import repository.impl.DoobieOrderRepositoryImpl

import java.util.UUID

trait OrderRepository[F[_]] {
  def updateOrder(mergedDomain: UpdateOrder): F[Int]

  def viewActiveOrders(): F[List[ReadOrder]]

  def createOrder(domain: CreateOrder): F[UUID]
}

object OrderRepository {
  def of[F[_]: Sync](tx: Transactor[F]): OrderRepository[F] = {
    new DoobieOrderRepositoryImpl(tx)
  }
}
