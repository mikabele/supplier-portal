package repository

import cats.data.NonEmptyList
import cats.effect.Async
import domain.attachment.AttachmentCreateDomain
import domain.criteria.CriteriaDomain
import domain.product.{ProductCreateDomain, ProductReadDomain, ProductStatus, ProductUpdateDomain}
import domain.user.AuthorizedUserDomain
import doobie.Transactor
import repository.impl.DoobieProductRepositoryImpl

import java.util.UUID

trait ProductRepository[F[_]] {
  def checkUniqueProduct(name: String, supplierId: Int): F[Option[UUID]]

  def getNewProductsBySubscription(user: AuthorizedUserDomain): F[List[ProductReadDomain]]

  def addProduct(product:    ProductCreateDomain): F[UUID]
  def updateProduct(product: ProductUpdateDomain): F[Int]
  def deleteProduct(id:      UUID): F[Int]
  def viewProducts(user:     AuthorizedUserDomain, statuses: NonEmptyList[ProductStatus]): F[List[ProductReadDomain]]
  def attach(attachment:     AttachmentCreateDomain): F[UUID]
  def searchByCriteria(user: AuthorizedUserDomain, criteria: CriteriaDomain): F[List[ProductReadDomain]]
  def getByIds(ids:          NonEmptyList[UUID]):  F[List[ProductReadDomain]]
  def removeAttachment(id:   UUID):                F[Int]

}

object ProductRepository {
  def of[F[_]: Async](tx: Transactor[F]): ProductRepository[F] = {
    new DoobieProductRepositoryImpl[F](tx)
  }
}
