package repository.impl

import cats.effect.Async
import domain.category.CategoryDomain
import doobie.implicits._
import doobie.refined.implicits._
import doobie.util.transactor.Transactor
import repository.CategoryRepository
import repository.impl.logger.logger._
import types._

class DoobieCategoryRepositoryImpl[F[_]: Async](tx: Transactor[F]) extends CategoryRepository[F] {

  private val getByIdQuery = fr"SELECT id,name FROM category "

  override def getById(id: PositiveInt): F[Option[CategoryDomain]] = {
    (getByIdQuery ++ fr" WHERE id = $id").query[CategoryDomain].option.transact(tx)
  }
}
