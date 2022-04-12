package repository

import cats.data.NonEmptyList
import cats.effect.Sync
import domain.attachment.AttachmentCreateDomain
import domain.criteria.CriteriaDomain
import domain.product._
import domain.user.ReadAuthorizedUser
import doobie.Transactor
import repository.impl.DoobieProductRepositoryImpl
import types.UuidStr

import java.util.UUID

trait ProductRepository[F[_]] {

  def addProduct(product:    ProductCreateDomain):   F[UUID]
  def updateProduct(product: ProductUpdateDomain):   F[Int]
  def deleteProduct(id:      UUID): F[Int]
  def viewProducts(user:     ReadAuthorizedUser, statuses: NonEmptyList[ProductStatus]): F[List[ProductReadDomain]]
  def attach(attachment:     AttachmentCreateDomain): F[UUID]
  def searchByCriteria(user: ReadAuthorizedUser, criteria: CriteriaDomain): F[List[ProductReadDomain]]
  def getByIds(ids:          NonEmptyList[UuidStr]): F[List[ProductReadDomain]]
  def removeAttachment(id:   UUID):                  F[Int]

}

object ProductRepository {
  def of[F[_]: Sync](tx: Transactor[F]): ProductRepository[F] = {
    new DoobieProductRepositoryImpl[F](tx)
  }
}
