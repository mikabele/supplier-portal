package repository

import cats.effect.Async
import domain.category.CategoryDomain
import doobie.util.transactor.Transactor
import repository.impl.DoobieCategoryRepositoryImpl
import types.PositiveInt

trait CategoryRepository[F[_]] {
  def getById(id: PositiveInt): F[Option[CategoryDomain]] //technical method
}

object CategoryRepository {
  def of[F[_]: Async](tx: Transactor[F]): CategoryRepository[F] = {
    new DoobieCategoryRepositoryImpl[F](tx)
  }
}
