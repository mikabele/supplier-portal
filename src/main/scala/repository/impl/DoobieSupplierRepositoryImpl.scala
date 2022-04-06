package repository.impl

import cats.effect.Sync
import domain.supplier.Supplier
import doobie.implicits._
import doobie.util.transactor.Transactor
import repository.SupplierRepository
import types.{PositiveInt, UuidStr}
import repository.impl.implicits._
import doobie.refined.implicits._ //never delete this row

class DoobieSupplierRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends SupplierRepository[F] {

  private val selectSupplierQuery = fr"SELECT id,name,address FROM supplier"

  override def getById(id: PositiveInt): F[Option[Supplier]] = {
    (selectSupplierQuery ++ fr" WHERE id = $id").query[Supplier].option.transact(tx)
  }
}
