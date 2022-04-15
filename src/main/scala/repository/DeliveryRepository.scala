package repository

import cats.effect.Sync
import domain.delivery.{DeliveryCreateDomain, DeliveryReadDomain}
import domain.user.AuthorizedUserDomain
import doobie.util.transactor.Transactor
import repository.impl.DoobieDeliveryRepositoryImpl

import java.util.UUID

trait DeliveryRepository[F[_]] {
  def delivered(courier: AuthorizedUserDomain, id: UUID): F[Int]

  def createDelivery(courier: AuthorizedUserDomain, domain: DeliveryCreateDomain): F[UUID]

  def showDeliveries(): F[List[DeliveryReadDomain]]
}

object DeliveryRepository {
  def of[F[_]: Sync](tx: Transactor[F]): DeliveryRepository[F] = {
    new DoobieDeliveryRepositoryImpl[F](tx)
  }
}
