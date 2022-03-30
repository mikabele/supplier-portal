package repository

trait ProductRepository[F[_]] {

  // TODO - add all methods (just to avoid some errors in Intellij)

}

object ProductRepository {
  def of[F[_]](params: Any*): ProductRepository[F] = ???
}
