package repository

import cats.effect.kernel.Sync
import domain.delivery.{DeliveryCreateDomain, DeliveryReadDomain}
import doobie.util.transactor.Transactor
import repository.impl.DoobieDeliveryRepositoryImpl

import java.util.UUID

trait DeliveryRepository[F[_]] {
  def delivered(id: UUID): F[Int]

  def createDelivery(courierId: UUID, domain: DeliveryCreateDomain): F[UUID]

  def showDeliveries(): F[List[DeliveryReadDomain]]
}

object DeliveryRepository {
  def of[F[_]: Sync](tx: Transactor[F]): DeliveryRepository[F] = {
    new DoobieDeliveryRepositoryImpl[F](tx)
  }
}
