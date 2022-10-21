package kafka

import cats.effect.{Async, Sync}
import cats.syntax.all._
import kafka.impl.KafkaProducerServiceImpl
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.Serializer

import scala.jdk.CollectionConverters._

trait KafkaProducerService[F[_], K, V] {
  def send(key: K, value: V): F[Unit]
}

object KafkaProducerService {
  def of[F[_]: Async, K, V](
    kafkaProducerConfig: Map[String, AnyRef],
    topic:               String
  )(
    implicit keySerializer: Serializer[K],
    valueSerializer:        Serializer[V]
  ): F[KafkaProducerServiceImpl[F, K, V]] = {
    for {
      kp <- Sync[F].delay(new KafkaProducer(kafkaProducerConfig.asJava, keySerializer, valueSerializer))
    } yield new KafkaProducerServiceImpl[F, K, V](kp, topic)
  }
}
