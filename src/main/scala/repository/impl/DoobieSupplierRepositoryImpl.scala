package repository.impl

import cats.effect.Sync
import domain.supplier.SupplierDomain
import doobie.implicits._
import doobie.util.transactor.Transactor
import repository.SupplierRepository
import types._
import doobie.refined.implicits._ //never delete this row
import repository.impl.logger.logger._

class DoobieSupplierRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends SupplierRepository[F] {

  private val selectSupplierQuery = fr"SELECT id,name,address FROM supplier"

  override def getById(id: PositiveInt): F[Option[SupplierDomain]] = {
    (selectSupplierQuery ++ fr" WHERE id = $id").query[SupplierDomain].option.transact(tx)
  }
}
