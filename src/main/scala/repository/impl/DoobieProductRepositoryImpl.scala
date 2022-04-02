package repository.impl

import cats.effect.Sync
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragments._
import java.util.UUID
import doobie.refined.implicits._ // never delete this row

import domain.attachment._
import domain.criteria._
import domain.product._
import repository.ProductRepository
import repository.impl.implicits._

class DoobieProductRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends ProductRepository[F] {

  private val addProductQuery    = fr"INSERT INTO product(name,category_id,supplier_id,price,description) VALUES ("
  private val updateProductQuery = fr"UPDATE product "
  private val deleteProductQuery = fr"DELETE FROM product "
  private val getProductsQuery =
    fr"SELECT p.id, p.name, p.category_id, s.id, s.name, s.address, p.price, p.description, p.status " ++
      fr"FROM product AS p " ++
      fr"INNER JOIN supplier AS s " ++
      fr"ON p.supplier_id = s.id "

  private val addAttachmentQuery = fr"INSERT INTO attachment(attachment,product_id) VALUES ("

  override def addProduct(product: CreateProduct): F[UUID] = {
    val fragment =
      addProductQuery ++
        fr"${product.name}, ${product.categoryId}, ${product.supplierId}, ${product.price}," ++
        fr"${product.description.getOrElse("")})"
    fragment.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(tx)
  }

  override def updateProduct(product: UpdateProduct): F[Int] = {
    val fragment = updateProductQuery ++
      set(
        fr"name = ${product.name}",
        fr"category_id = ${product.categoryId}",
        fr"supplier_id = ${product.supplierId}",
        fr"price = ${product.price}",
        fr"description = ${product.description}",
        fr"status = ${product.status}::product_status"
      ) ++
      fr"WHERE id = ${product.id}::UUID"
    fragment.update.run.transact(tx)
  }

  override def deleteProduct(id: UUID): F[Int] = {
    (deleteProductQuery ++ fr"WHERE id = $id").update.run.transact(tx)
  }

  override def viewProducts(): F[List[ReadProduct]] = {
    getProductsQuery.query[ReadProduct].to[List].transact(tx)
  }

  override def attach(attachment: CreateAttachment): F[UUID] = {
    (addAttachmentQuery ++ fr"${attachment.attachment}, ${attachment.productId}::UUID)").update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(tx)
  }

  override def searchByCriteria(criteria: Criteria): F[List[ReadProduct]] = {
    (getProductsQuery ++ whereAndOpt(
      criteria.id.map(value => fr"p.id = $value::UUID"),
      criteria.name.map(value => fr"p.name = $value"),
      criteria.categoryId.map(value => fr"p.category_id = $value"),
      criteria.description.map(value => fr"p.description = $value"),
      criteria.supplierId.map(value => fr"p.supplier_id = $value"),
      criteria.publicationPeriod.map(value => fr"p.publication_period = $value")
    ))
      .query[ReadProduct]
      .to[List]
      .transact(tx)
  }
}
