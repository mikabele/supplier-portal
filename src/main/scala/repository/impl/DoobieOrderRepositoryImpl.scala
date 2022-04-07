package repository.impl

import cats.data.NonEmptyList
import cats.effect.Sync
import cats.syntax.all._
import domain.order._
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._ // never delete this row
import doobie.util.fragments._
import repository.OrderRepository
import repository.impl.logger.logger._
import util.ModelMapper.DbModelMapper._

import java.util.UUID

class DoobieOrderRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends OrderRepository[F] {

  private val updateOrderQuery = fr"UPDATE supplier_portal.public.order"
  private val selectOrdersQuery =
    fr"SELECT o.id,o.user_id,o.status, o.ordered_start_date, o.total,o.address " ++
      fr"FROM supplier_portal.public.order AS o "

  private val selectOrderProductsQuery = fr"SELECT order_id,product_id,count FROM order_to_product "

  private val insertOrderQuery      = fr"INSERT INTO supplier_portal.public.order(user_id,total,address) VALUES ("
  private val insertOrderItemsQuery = fr"INSERT INTO order_to_product(order_id,product_id,count) SELECT "

  override def viewActiveOrders(): F[List[ReadOrder]] = {
    for {
      orders <- (selectOrdersQuery
        ++ fr" WHERE o.status != 'cancelled'::order_status")
        .query[DbReadOrder]
        .to[List]
        .transact(tx)

      orderProducts <- getOrderProducts(orders)
    } yield joinOrdersWithProducts(orders, orderProducts)
  }

  private def getOrderProducts(orders: List[DbReadOrder]): F[List[ReadOrderItem]] = {
    NonEmptyList
      .fromList(orders)
      .map(nel =>
        (selectOrderProductsQuery ++ fr" WHERE " ++ in(fr"order_id", nel.map(o => UUID.fromString(o.id.value))))
          .query[ReadOrderItem]
          .to[List]
          .transact(tx)
      )
      .getOrElse(List.empty[ReadOrderItem].pure[F])
  }

  override def createOrder(domain: CreateOrder): F[UUID] = {
    val res = for {
      id <- (insertOrderQuery ++ fr"${domain.userId}::UUID,${domain.total},${domain.address})").update
        .withUniqueGeneratedKeys[UUID]("id")
      _ <-
        (insertOrderItemsQuery ++ fr"$id,unnest(${domain.orderItems.map(_.productId.value)}::UUID[]),unnest(${domain.orderItems
          .map(_.count.value)})").update.run
    } yield id

    res.transact(tx)
  }

  override def cancelOrder(id: UUID): F[Int] = {
    (updateOrderQuery ++ set(
      fr"status = 'cancelled'::order_status"
    ) ++ fr" WHERE id = $id").update.run.transact(tx)
  }
}
