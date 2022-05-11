package service

import cats.effect.{Async, ContextShift, Timer}
import cats.syntax.all._
import cats.{Monad, MonadError}
import conf.app.EmailNotificatorConf
import emil.Emil
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
    emil:                   Emil[F],
    host:                   EmailNotificatorConf,
    productRepository:      ProductRepository[F],
    userRepository:         UserRepository[F],
    subscriptionRepository: SubscriptionRepository[F],
    groupRepository:        ProductGroupRepository[F],
    productKafkaConsumer:   KafkaConsumerService[F, String, UUID]
  )(
    implicit logHandler: LogHandler[F]
  ): EmailNotificationService[F] = {
    new EmailNotificationServiceImpl[F](
      emil,
      host,
      productRepository,
      userRepository,
      subscriptionRepository,
      groupRepository,
      logHandler,
      productKafkaConsumer
    )
  }
}
