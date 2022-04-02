package repository.impl

import doobie.{Read, Write}
import types._
import util.CaseConversionUtil._
import doobie.refined.implicits._ // never delete this row

import domain.category.Category
import domain.product.{ProductStatus, ReadProduct}
import domain.supplier.Supplier

package object implicits {
  implicit val readProductStatus: Read[ProductStatus] = Read[String].map(v => {
    val t = snakeToCamel(v)
    ProductStatus.of(snakeToCamel(v))
  })
  implicit val writeProductStatus: Write[ProductStatus] = Write[String].contramap(v => {
    val t = camelToSnake(v.toString)
    camelToSnake(v.toString)
  })

  implicit val readCategory:  Read[Category]  = Read[Int].map(id => Category.of(id))
  implicit val writeCategory: Write[Category] = Write[Int].contramap(c => c.id)

  implicit def readProductRead: Read[ReadProduct] =
    Read[(UuidStr, NonEmptyStr, Category, Supplier, NonNegativeFloat, String, ProductStatus)]
      .map { case (product_id, product_name, category, supplier, price, description, status) =>
        ReadProduct(product_id, product_name, category, supplier, price, description, status)
      }
}
