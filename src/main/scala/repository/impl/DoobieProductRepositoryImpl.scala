package repository.impl

import cats.data.NonEmptyList
import cats.effect.Sync
import domain.attachment._
import domain.criteria._
import domain.product._
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._ // never delete this row
import doobie.util.fragments._
import repository.ProductRepository
import repository.impl.implicits._
import types.UuidStr

import java.util.UUID

class DoobieProductRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends ProductRepository[F] {

  private val addProductQuery    = fr"INSERT INTO product(name,category_id,supplier_id,price,description) VALUES ("
  private val updateProductQuery = fr"UPDATE product "
  private val deleteProductQuery = fr"DELETE FROM product "
  private val getProductsQuery =
    fr"SELECT p.id, p.name, p.category_id, s.id, s.name, s.address, p.price, p.description, p.status,p.publication_period, " ++
      fr"COALESCE(array_agg(a.id),'{}'),COALESCE(array_agg(a.attachment),'{}') " ++
      fr"FROM product AS p " ++
      fr"INNER JOIN supplier AS s " ++
      fr"ON p.supplier_id = s.id " ++
      fr"LEFT JOIN attachment AS a ON a.product_id = p.id "

  private val addAttachmentQuery    = fr"INSERT INTO attachment(attachment,product_id) VALUES ("
  private val removeAttachmentQuery = fr"DELETE FROM attachment "

  override def addProduct(product: CreateProduct): F[UUID] = {
    val fragment =
      addProductQuery ++
        fr"${product.name}, ${product.category}, ${product.supplierId}, ${product.price}," ++
        fr"${product.description.getOrElse("")})"
    fragment.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(tx)
  }

  override def updateProduct(product: UpdateProduct): F[Int] = {
    val fragment = updateProductQuery ++
      set(
        fr"name = ${product.name}",
        fr"category_id = ${product.category}",
        fr"supplier_id = ${product.supplierId}",
        fr"price = ${product.price}",
        fr"description = ${product.description}",
        fr"status = ${product.status}"
      ) ++
      fr"WHERE id = ${product.id}::UUID"
    fragment.update.run.transact(tx)
  }

  override def deleteProduct(id: UUID): F[Int] = {
    (deleteProductQuery ++ fr"WHERE id = $id").update.run.transact(tx)
  }

  override def viewProducts(statuses: NonEmptyList[ProductStatus]): F[List[ReadProduct]] = {
    (getProductsQuery ++ fr" WHERE " ++ in(
      fr"status",
      statuses
    ) ++ fr"GROUP BY p.id, p.name, p.category_id, s.id, s.name, s.address, p.price, p.description, p.status,p.publication_period")
      .query[ReadProduct]
      .to[List]
      .transact(tx)
  }

  override def attach(attachment: CreateAttachment): F[UUID] = {
    (addAttachmentQuery ++ fr"${attachment.attachment}, ${attachment.productId}::UUID)").update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(tx)
  }

  override def searchByCriteria(criteria: Criteria): F[List[ReadProduct]] = {
    (getProductsQuery ++ fr" INNER JOIN category AS c ON c.id=p.category_id " ++ whereAndOpt(
      criteria.name.map(value => fr"p.name LIKE $value"),
      criteria.categoryName.map(value => fr"c.name LIKE $value"),
      criteria.description.map(value => fr"p.description LIKE $value"),
      criteria.supplierName.map(value => fr"s.name LIKE $value"),
      criteria.status.map(value => fr"p.status = $value"),
      criteria.startDate.map(value => fr"p.publication_period >= $value::DATE"),
      criteria.endDate.map(value => fr"p.publication_period < $value::DATE")
    ) ++ fr" GROUP BY p.id, p.name, p.category_id, s.id, s.name, s.address, p.price, p.description, p.status,p.publication_period")
      .query[ReadProduct]
      .to[List]
      .transact(tx)
  }

  override def getByIds(ids: NonEmptyList[UuidStr]): F[List[ReadProduct]] = {
    val modifiedIds = ids.map(id => UUID.fromString(id.value))
    (getProductsQuery ++ whereAnd(
      in(fr"p.id", modifiedIds)
    ) ++ fr"GROUP BY p.id, p.name, p.category_id, s.id, s.name, s.address, p.price, p.description, p.status,p.publication_period")
      .query[ReadProduct]
      .to[List]
      .transact(tx)
  }

  override def removeAttachment(id: UUID): F[Int] = {
    (removeAttachmentQuery ++ fr"WHERE id = $id").update.run.transact(tx)
  }
}
