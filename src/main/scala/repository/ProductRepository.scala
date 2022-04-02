package repository

import cats.effect.Sync
import domain.attachment.CreateAttachment
import domain.criteria.Criteria
import domain.product._
import doobie.Transactor
import repository.impl.DoobieProductRepositoryImpl

import java.util.UUID

trait ProductRepository[F[_]] {

  def addProduct(product:    CreateProduct): F[UUID]
  def updateProduct(product: UpdateProduct): F[Int]
  def deleteProduct(id:      UUID):          F[Int]
  def viewProducts(): F[List[ReadProduct]]
  def attach(attachment:         CreateAttachment): F[UUID]
  def searchByCriteria(criteria: Criteria):         F[List[ReadProduct]]

}

object ProductRepository {
  def of[F[_]: Sync](tx: Transactor[F]): ProductRepository[F] = {
    new DoobieProductRepositoryImpl[F](tx)
  }
}
