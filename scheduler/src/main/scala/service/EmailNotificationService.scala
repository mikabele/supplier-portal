package service

import cats.effect.{Async, Blocker, ContextShift, Timer}
import cats.{Monad, MonadError}
import conf.app.EmailNotificatorConf
import kafka.KafkaConsumerService
import logger.LogHandler
import repository.{ProductGroupRepository, ProductRepository, SubscriptionRepository, UserRepository}
import service.impl.EmailNotificationServiceImpl

import java.util.UUID

trait EmailNotificationService[F[_]] {
  def start(): F[Any]
}

object EmailNotificationService {
  def of[F[+_]: Async: Monad: MonadError[*[_], Throwable]: Timer: ContextShift](
    host:                   EmailNotificatorConf,
    productRepository:      ProductRepository[F],
    userRepository:         UserRepository[F],
    subscriptionRepository: SubscriptionRepository[F],
    groupRepository:        ProductGroupRepository[F],
    be:                     Blocker,
    logger:                 LogHandler[F],
    productKafkaConsumer:   KafkaConsumerService[F, String, UUID]
  ): EmailNotificationService[F] = {
    new EmailNotificationServiceImpl[F](
      host,
      productRepository,
      userRepository,
      subscriptionRepository,
      groupRepository,
      be,
      logger,
      productKafkaConsumer
    )
  }
}
