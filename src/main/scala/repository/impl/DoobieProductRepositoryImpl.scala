package repository.impl

import cats.data.NonEmptyList
import cats.syntax.all._
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
import types.UuidStr
import repository.impl.logger.logger._
import util.ModelMapper.DbModelMapper._

import java.util.UUID

class DoobieProductRepositoryImpl[F[_]: Sync](tx: Transactor[F]) extends ProductRepository[F] {

  private val addProductQuery    = fr"INSERT INTO product(name,category_id,supplier_id,price,description) VALUES ("
  private val updateProductQuery = fr"UPDATE product "
  private val deleteProductQuery = fr"DELETE FROM product "
  private val getProductsQuery =
    fr"SELECT p.id, p.name, p.category_id, s.id, s.name, s.address, p.price, p.description, p.status,p.publication_period " ++
      fr"FROM product AS p " ++
      fr"INNER JOIN supplier AS s " ++
      fr"ON p.supplier_id = s.id "

  private val getAttachmentsQuery = fr"SELECT id,product_id,attachment FROM attachment "

  private val addAttachmentQuery    = fr"INSERT INTO attachment(attachment,product_id) VALUES ("
  private val removeAttachmentQuery = fr"DELETE FROM attachment "
  private val findUserInGroupQuery = fr" p.id IN (select gtp.product_id FROM group_to_product AS gtp " ++
    fr"INNER JOIN public.group AS g ON g.id = gtp.group_id INNER JOIN group_to_user AS gtu ON g.id=gtu.group_id "

  // TODO - implement logic where new product appeared need to insert new record in product groups in superadmin group(managers should have access to all products)
  override def addProduct(product: ProductCreateDomain): F[UUID] = {
    val fragment =
      addProductQuery ++
        fr"${product.name}, ${product.category}, ${product.supplierId}, ${product.price}," ++
        fr"${product.description.getOrElse("")})"
    fragment.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(tx)
  }

  override def updateProduct(product: ProductUpdateDomain): F[Int] = {
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

  override def viewProducts(userId: UUID, statuses: NonEmptyList[ProductStatus]): F[List[ProductReadDomain]] = {
    for {
      products <- (getProductsQuery ++ fr" WHERE " ++ in(
        fr"status",
        statuses
      ) ++ fr" AND " ++ findUserInGroupQuery ++ fr" WHERE gtu.user_id = $userId)")
        .query[ProductReadDbDomain]
        .to[List]
        .transact(tx)
      attachments <- getAttachments(products)
    } yield joinProductsWithAttachments(products, attachments)
  }

  override def attach(attachment: AttachmentCreateDomain): F[UUID] = {
    (addAttachmentQuery ++ fr"${attachment.attachment}, ${attachment.productId}::UUID)").update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(tx)
  }

  private def getAttachments(products: List[ProductReadDbDomain]): F[List[AttachmentReadDomain]] = {
    NonEmptyList
      .fromList(products)
      .map(nel =>
        (getAttachmentsQuery ++ fr" WHERE " ++ in(fr"product_id", nel.map(p => UUID.fromString(p.id.value))))
          .query[AttachmentReadDomain]
          .to[List]
          .transact(tx)
      )
      .getOrElse(List.empty[AttachmentReadDomain].pure[F])
  }

  override def searchByCriteria(userId: UUID, criteria: CriteriaDomain): F[List[ProductReadDomain]] = {
    for {
      products <- (getProductsQuery ++ fr" INNER JOIN category AS c ON c.id=p.category_id " ++ whereAndOpt(
        criteria.name.map(value => fr"p.name LIKE $value"),
        criteria.categoryName.map(value => fr"c.name LIKE $value"),
        criteria.description.map(value => fr"p.description LIKE $value"),
        criteria.supplierName.map(value => fr"s.name LIKE $value"),
        criteria.status.map(value => fr"p.status = $value"),
        criteria.startDate.map(value => fr"p.publication_period >= $value::DATE"),
        criteria.endDate.map(value => fr"p.publication_period < $value::DATE")
      ) ++ fr" AND " ++ findUserInGroupQuery ++ fr" WHERE gtu.user_id = $userId)")
        .query[ProductReadDbDomain]
        .to[List]
        .transact(tx)
      attachments <- getAttachments(products)
    } yield joinProductsWithAttachments(products, attachments)
  }

  // technical method - use in order and group services
  override def getByIds(ids: NonEmptyList[UuidStr]): F[List[ProductReadDomain]] = {
    val modifiedIds = ids.map(id => UUID.fromString(id.value))
    for {
      products <- (getProductsQuery ++ fr" WHERE " ++ in(fr"p.id", modifiedIds))
        .query[ProductReadDbDomain]
        .to[List]
        .transact(tx)
      attachments <- getAttachments(products)
    } yield joinProductsWithAttachments(products, attachments)
  }

  override def removeAttachment(id: UUID): F[Int] = {
    (removeAttachmentQuery ++ fr"WHERE id = $id").update.run.transact(tx)
  }
}
