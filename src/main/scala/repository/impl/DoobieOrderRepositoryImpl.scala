package repository.impl

import cats.effect.Sync
import cats.syntax.all._
import doobie.{Fragment, Transactor, Update}
import doobie.implicits._
import doobie.postgres._
import doobie.postgres.implicits._
import doobie.util.fragments._

import java.util.UUID
import doobie.refined.implicits._ //never delete this row
import repository.OrderRepository
import domain.order._
import types._
import repository.impl.implicits._ //never delete this row
import doobie.hi._

// TODO - read how to use \" in fragments to use reserved keywords in queries UPD: right now i need to use full path to table
// TODO - try to use object type in postgres

class DoobieOrderRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends OrderRepository[F] {

  private val updateOrderQuery = fr"UPDATE supplier_portal.public.order"
  private val selectOrdersQuery =
    fr"SELECT o.id, array_agg(otp.product_id),array_agg(otp.count), o.status, o.ordered_start_date, o.total " ++
      fr"FROM supplier_portal.public.order AS o INNER JOIN order_to_product AS otp ON o.id=otp.order_id " ++
      fr"GROUP BY o.id, o.status, o.ordered_start_date, o.total"

  private val callCreateOrderFunction = fr"SELECT create_order("

  override def updateOrder(domain: UpdateOrder): F[Int] = {
    (updateOrderQuery ++ set(
      fr"status = ${domain.orderStatus}::ORDER_STATUS"
    )).update.run.transact(tx)
  }

  override def viewActiveOrders(): F[List[ReadOrder]] = {
    selectOrdersQuery
      .query[ReadOrder]
      .to[List]
      .transact(tx)
  }

  // TODO - create transaction inside function

  override def createOrder(domain: CreateOrder): F[UUID] = {
    (callCreateOrderFunction ++ fr"${domain.userId}::UUID,${domain.orderItems
      .map(_.productId.value)}::UUID[],${domain.orderItems.map(_.count.value)})")
      .query[UUID]
      .unique
      .transact(tx)
  }
}
