package repository

import cats.effect.Sync
import domain.supplier.SupplierDomain
import doobie.Transactor
import repository.impl.DoobieSupplierRepositoryImpl
import types.PositiveInt

trait SupplierRepository[F[_]] {
  def getById(id: PositiveInt): F[Option[SupplierDomain]]
}

object SupplierRepository {
  def of[F[_]: Sync](tx: Transactor[F]): SupplierRepository[F] = {
    new DoobieSupplierRepositoryImpl[F](tx)
  }
}
