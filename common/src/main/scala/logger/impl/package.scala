package logger

import cats.Applicative
import cats.implicits.catsSyntaxApplicativeId
import org.apache.logging.log4j.LogManager

package object impl {
  def log4jLogHandler[F[_]: Applicative](layerName: String): LogHandler[F] = {
    val logger = LogManager.getLogger(layerName)
    LogHandler.of(
      (s: String) => logger.info(s).pure[F],
      (s: String) => logger.debug(s).pure[F],
      (s: String) => logger.error(s).pure[F]
    )
  }

  def dummy[F[_]: Applicative]: LogHandler[F] = LogHandler.of(
    (s: String) => println(s).pure[F],
    (s: String) => println(s).pure[F],
    (s: String) => println(s).pure[F]
  )
}
