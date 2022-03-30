package repository.impl

import cats.effect.Sync
import domain.attachment._
import domain.criteria._
import domain.product._
import doobie.{Fragment, Transactor}
import doobie.implicits._
import repository.ProductRepository

import java.util.UUID

// TODO - withUniqueGeneratedKeys - what the hell happened with ambiguous implicits

class DoobieProductRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends ProductRepository[F] {

  private val addProductQuery    = fr"INSERT INTO products(name,category_id,supplier_id,price,description) VALUES ("
  private val updateProductQuery = fr"UPDATE products SET "
  private val deleteProductQuery = fr"DELETE FROM products "
  private val getProductsQuery =
    fr"SELECT p.id, p.name, p.category_id, s.id, s.name, s.address, p.price, p.description, p.status " ++
      fr"FROM products AS p " ++
      fr"INNER JOIN suppliers AS s " ++
      fr"ON p.supplier_id = s.id "

  private val addAttachmentQuery = fr"INSERT INTO attachments(attachment,product_id) VALUES ("

  override def addProduct(product: CreateProduct): F[UUID] = {
    (addProductQuery ++
      fr"${product.name}, ${product.categoryId}, ${product.supplierId}, ${product.price}, ${product.description
        .getOrElse("")})").update.withUniqueGeneratedKeys[UUID]("id").transact(tx)
  }

  override def updateProduct(product: UpdateProduct): F[Int] = {
    (updateProductQuery ++
      parseOptionField("name", product.name) ++ fr"AND " ++
      parseOptionField("category_id", product.categoryId) ++ fr"AND " ++
      parseOptionField("supplier_id", product.supplierId) ++ fr"AND " ++
      parseOptionField("price", product.price) ++ fr"AND " ++
      parseOptionField("description", product.description) ++ fr"AND " ++
      parseOptionField("status", product.status) ++
      fr"WHERE id = ${product.id}").update.run.transact(tx)
  }

  private def parseOptionField[A](fieldName: String, fieldValue: Option[A]): Fragment = {
    fieldValue.map(v => fr"$fieldName = $v ").getOrElse(fr"")
  }

  override def deleteProduct(id: UUID): F[Int] = {
    (deleteProductQuery ++ fr"WHERE id = $id").update.run.transact(tx)
  }

  override def viewProducts(): F[List[ReadProduct]] = {
    getProductsQuery.query[ReadProduct].to[List].transact(tx)
  }

  override def attach(attachment: CreateAttachment): F[UUID] = {
    (addAttachmentQuery ++ fr"${attachment.attachment}, ${attachment.productId})").update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(tx)
  }

  override def searchByCriteria(criteria: Criteria): F[List[ReadProduct]] = {
    (getProductsQuery ++ fr"WHERE " ++
      parseOptionField("name", criteria.name) ++ fr"AND " ++
      parseOptionField("category_id", criteria.categoryId) ++ fr"AND " ++
      parseOptionField("description", criteria.description) ++ fr"AND " ++
      parseOptionField("supplier_id", criteria.supplierId) ++ fr"AND " ++
      parseOptionField("publication_period", criteria.publicationPeriod)).query[ReadProduct].to[List].transact(tx)
  }
}
