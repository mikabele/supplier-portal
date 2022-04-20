package logger

trait LogHandler[F[_]] {
  def info(message:  String): F[Unit]
  def debug(message: String): F[Unit]
  def error(message: String): F[Unit]
}

object LogHandler {
  def of[F[_]](infoF: String => F[Unit], debugF: String => F[Unit], errorF: String => F[Unit]): LogHandler[F] = {
    new LogHandler[F] {
      override def info(message: String): F[Unit] = infoF(message)

      override def debug(message: String): F[Unit] = debugF(message)

      override def error(message: String): F[Unit] = errorF(message)
    }
  }
}
