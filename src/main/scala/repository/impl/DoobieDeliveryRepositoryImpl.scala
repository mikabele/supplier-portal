package repository.impl

import cats.effect.Sync
import cats.syntax.all._
import domain.delivery._
import domain.user.ReadAuthorizedUser
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import repository.DeliveryRepository
import repository.impl.logger.logger._

import java.util.UUID

class DoobieDeliveryRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends DeliveryRepository[F] {

  private val selectDeliveriesQuery =
    fr"SELECT d.id,d.order_id,c.id,c.name,c.surname,c.role,c.phone,c.email,d.delivery_start_date,d.delivery_finish_date " ++
      fr"FROM delivery AS d INNER JOIN public.user AS c ON d.courier_id=c.id "

  private val createDeliveryQuery    = fr"INSERT INTO delivery(courier_id,order_id) VALUES "
  private val updateOrderStatusQuery = fr"UPDATE public.order SET status = "
  private val updateDeliveryQuery    = fr"UPDATE delivery "

  override def delivered(courier: ReadAuthorizedUser, id: UUID): F[Int] = {
    val res = for {
      c <-
        (updateDeliveryQuery ++ fr"SET delivery_finish_date = CURRENT_DATE" ++
          fr" WHERE order_id = $id AND courier_id = ${courier.id}::UUID").update.run
      count <- (updateOrderStatusQuery ++ fr"'delivered'::order_status" ++ fr" WHERE id = $id AND $c>0").update.run
    } yield count

    res.transact(tx)
  }

  override def createDelivery(courier: ReadAuthorizedUser, domain: DeliveryCreateDomain): F[UUID] = {
    val res = for {
      id <- (createDeliveryQuery ++ fr"(${courier.id}::UUID,${domain.orderId}::UUID)").update
        .withUniqueGeneratedKeys[UUID]("id")
      _ <-
        (updateOrderStatusQuery ++ fr"'assigned'::order_status" ++ fr" WHERE id = ${domain.orderId}::UUID").update.run
    } yield id

    res.transact(tx)
  }

  override def showDeliveries(): F[List[DeliveryReadDomain]] = {
    selectDeliveriesQuery.query[DeliveryReadDomain].to[List].transact(tx)
  }
}
