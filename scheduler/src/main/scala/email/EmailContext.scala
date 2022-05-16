package email

import cats.effect.{Async, Blocker, ContextShift, Timer}
import cats.{Monad, MonadError}
import emil.Emil
import emil.javamail.JavaMailEmil

object EmailContext {
  def of[F[_]: Async: Monad: MonadError[*[_], Throwable]: Timer: ContextShift](
    be: Blocker
  ): F[Emil[F]] = {
    Async[F].delay(JavaMailEmil[F](be))
  }
}
