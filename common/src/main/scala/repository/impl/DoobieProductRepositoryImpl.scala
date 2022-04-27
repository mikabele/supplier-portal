package repository.impl

import cats.data.NonEmptyList
import cats.effect.Async
import cats.syntax.all._
import domain.attachment.{AttachmentCreateDomain, AttachmentReadDomain}
import domain.criteria.CriteriaDomain
import domain.product._
import domain.user.{AuthorizedUserDomain, Role}
import doobie.Transactor
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.refined.implicits._
import doobie.util.fragment.Fragment
import doobie.util.fragments._
import repository.ProductRepository
import repository.impl.logger.logger._
import types.UuidStr
import util.ModelMapper.DbModelMapper._

import java.util.UUID

class DoobieProductRepositoryImpl[F[_]: Async](tx: Transactor[F]) extends ProductRepository[F] {

  private val addProductQuery    = fr"INSERT INTO product(name,category_id,supplier_id,price,description) VALUES ("
  private val updateProductQuery = fr"UPDATE product "
  private val deleteProductQuery = fr"DELETE FROM product "
  private val getProductsQuery =
    fr"SELECT p.id, p.name, c.id, c.name, s.id, s.name, s.address, p.price, p.description, p.status,TO_CHAR(p.publication_date,'yyyy-MM-dd HH:mm:ss') " ++
      fr"FROM product AS p " ++
      fr"INNER JOIN category AS c " ++
      fr"ON p.category_id = c.id" ++
      fr"INNER JOIN supplier AS s " ++
      fr"ON p.supplier_id = s.id "

  private val getAttachmentsQuery = fr"SELECT id,product_id,attachment FROM attachment "

  private val addAttachmentQuery    = fr"INSERT INTO attachment(attachment,product_id) VALUES ("
  private val removeAttachmentQuery = fr"DELETE FROM attachment "
  private val findUserInGroupQuery = fr" LEFT JOIN ( " ++
    fr"SELECT gtp.product_id,g.id " ++
    fr"FROM group_to_product AS gtp " ++
    fr"  INNER JOIN public.group AS g " ++
    fr"ON g.id = gtp.group_id   ) AS gtpu " ++
    fr"ON p.id=gtpu.product_id " ++
    fr"WHERE (gtpu.id IS NULL OR gtpu.id IN ( " ++
    fr"SELECT group_id FROM group_to_user "

  private val deleteGroupProductQuery = fr"DELETE FROM group_to_product "
  private val deleteFromOrderQuery    = fr"DELETE FROM order_to_product "
  private val checkUniqueProductQuery = fr"SELECT id FROM product"

  override def addProduct(product: ProductCreateDomain): F[UUID] = {
    val fragment =
      addProductQuery ++
        fr"${product.name}, ${product.categoryId}, ${product.supplierId}, ${product.price}," ++
        fr"${product.description.getOrElse("")})"
    fragment.update
      .withUniqueGeneratedKeys[UUID]("id")
      .transact(tx)
  }

  override def updateProduct(product: ProductUpdateDomain): F[Int] = {
    val fragment = updateProductQuery ++
      set(
        fr"name = ${product.name}",
        fr"category_id = ${product.categoryId}",
        fr"supplier_id = ${product.supplierId}",
        fr"price = ${product.price}",
        fr"description = ${product.description}",
        fr"status = ${product.status}",
        fr"publication_date = CURRENT_DATE"
      ) ++
      fr"WHERE id = ${product.id}::UUID"
    fragment.update.run.transact(tx)
  }

  override def deleteProduct(id: UUID): F[Int] = {
    val res = for {
      _     <- (deleteGroupProductQuery ++ fr" WHERE product_id = $id").update.run
      _     <- (deleteFromOrderQuery ++ fr" WHERE product_id = $id").update.run
      _     <- (removeAttachmentQuery ++ fr" WHERE product_id = $id").update.run
      count <- (deleteProductQuery ++ fr"WHERE id = $id").update.run
    } yield count

    res.transact(tx)
  }

  override def viewProducts(
    user:     AuthorizedUserDomain,
    statuses: NonEmptyList[ProductStatus]
  ): F[List[ProductReadDomain]] = {
    val ifNotManager =
      if (user.role != Role.Manager)
        findUserInGroupQuery ++ fr" WHERE user_id = ${user.id}::UUID))" ++ fr" AND " ++ in(
          fr"status",
          statuses
        )
      else
        Fragment.empty
    for {
      products <- (getProductsQuery ++ ifNotManager)
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

  override def searchByCriteria(user: AuthorizedUserDomain, criteria: CriteriaDomain): F[List[ProductReadDomain]] = {
    for {
      products <-
        (getProductsQuery ++ findUserInGroupQuery ++ fr" WHERE user_id = ${user.id}::UUID))" ++ fr" AND " ++ andOpt(
          criteria.name.map(value => fr"p.name LIKE $value"),
          criteria.categoryName.map(value => fr"c.name LIKE $value"),
          criteria.description.map(value => fr"p.description LIKE $value"),
          criteria.supplierName.map(value => fr"s.name LIKE $value"),
          criteria.minPrice.map(value => fr"p.price >= $value"),
          criteria.maxPrice.map(value => fr"p.price <= $value"),
          criteria.startDate.map(value => fr"p.publication_date >= TO_TIMESTAMP($value,'yyyy-MM-dd HH:mm:ss')"),
          criteria.endDate.map(value => fr"p.publication_date < TO_TIMESTAMP($value,'yyyy-MM-dd HH:mm:ss')")
        ) ++ fr" AND p.status IN ('available'::product_status,'not_available'::product_status)")
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

  override def getNewProductsBySubscription(user: AuthorizedUserDomain): F[List[ProductReadDomain]] = {
    for {
      products <- (getProductsQuery ++
        findUserInGroupQuery ++ fr" WHERE user_id = ${user.id}::UUID))" ++ fr" AND p.status IN ('available'::product_status,'not_available'::product_status) "
        ++ fr" AND (p.category_id IN (SELECT category_id FROM category_subscription WHERE user_id = ${user.id}::UUID) "
        ++ fr"OR p.supplier_id IN (SELECT supplier_id FROM supplier_subscription WHERE user_id = ${user.id}::UUID)) "
        ++ fr"AND p.publication_date >= (SELECT last_date FROM last_notification)")
        .query[ProductReadDbDomain]
        .to[List]
        .transact(tx)
      attachments <- getAttachments(products)
    } yield joinProductsWithAttachments(products, attachments)
  }

  override def checkUniqueProduct(name: String, supplierId: Int): F[Option[UUID]] = {
    (checkUniqueProductQuery ++ fr" WHERE name = $name AND supplier_id = $supplierId").query[UUID].option.transact(tx)
  }
}
