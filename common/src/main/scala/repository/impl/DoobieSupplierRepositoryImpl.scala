package repository.impl

import cats.effect.Async
import domain.supplier.SupplierDomain
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.transactor.Transactor
import repository.SupplierRepository
import repository.impl.logger.logger._
import types._

class DoobieSupplierRepositoryImpl[F[_]: Async](tx: Transactor[F]) extends SupplierRepository[F] {

  private val selectSupplierQuery = fr"SELECT id,name,address FROM supplier"

  override def getById(id: PositiveInt): F[Option[SupplierDomain]] = {
    (selectSupplierQuery ++ fr" WHERE id = $id").query[SupplierDomain].option.transact(tx)
  }
}
