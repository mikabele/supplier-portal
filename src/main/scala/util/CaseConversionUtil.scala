package util

object CaseConversionUtil {

  /** Takes a camel cased identifier name and returns an underscore separated
    * name
    *
    * Example:
    *     camelToUnderscores("ThisIsA1Test") == "this_is_a_1_test"
    */
  def camelToSnake(name: String): String =
    "_?[A-Z][a-z\\d]+".r.findAllMatchIn(name).map(_.group(0).toLowerCase).mkString("_")

  /** Takes an underscore separated identifier name and returns a camel cased one
    *
    * Example:
    *    underscoreToCamel("this_is_a_1_test") == "thisIsA1Test"
    */

  def snakeToCamel(name: String): String =
    "_([a-z\\d])".r
      .replaceAllIn(name, _.group(1).toUpperCase())
}
