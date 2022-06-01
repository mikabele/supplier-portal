package repository

import cats.effect.Async
import domain.supplier.SupplierDomain
import doobie.Transactor
import repository.impl.DoobieSupplierRepositoryImpl
import types.PositiveInt

trait SupplierRepository[F[_]] {
  def getById(id: PositiveInt): F[Option[SupplierDomain]] //technical method
}

object SupplierRepository {
  def of[F[_]: Async](tx: Transactor[F]): SupplierRepository[F] = {
    new DoobieSupplierRepositoryImpl[F](tx)
  }
}
