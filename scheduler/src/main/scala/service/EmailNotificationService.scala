package service

import cats.{Monad, MonadError}
import cats.effect.{Async, Blocker, ContextShift, Timer}
import conf.app.EmailNotificatorConf
import logger.LogHandler
import repository.{ProductRepository, SubscriptionRepository, UserRepository}
import service.impl.EmailNotificationServiceImpl

trait EmailNotificationService[F[_]] {
  def start(): F[Any]
}

object EmailNotificationService {
  def of[F[+_]: Async: Monad: MonadError[*[_], Throwable]: Timer: ContextShift](
    host:                   EmailNotificatorConf,
    productRepository:      ProductRepository[F],
    userRepository:         UserRepository[F],
    subscriptionRepository: SubscriptionRepository[F],
    be:                     Blocker,
    logger:                 LogHandler[F]
  ): EmailNotificationService[F] = {
    new EmailNotificationServiceImpl[F](host, productRepository, userRepository, subscriptionRepository, be, logger)
  }
}
