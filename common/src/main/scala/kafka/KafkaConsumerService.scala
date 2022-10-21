package kafka

import cats.Monad
import cats.effect.Async
import cats.syntax.all._
import kafka.impl.KafkaConsumerServiceImpl
import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}
import org.apache.kafka.common.serialization.Deserializer

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

trait KafkaConsumerService[F[_], K, V] {
  def poll(topic: String, pollingTimeout: FiniteDuration): F[List[ConsumerRecord[K, V]]] // pull data from broker queue

  def subscribe(topics: List[String]): F[Unit] // subscribe to topic
}

object KafkaConsumerService {
  def of[F[_]: Monad: Async, K, V](
    kafkaConsumerConfig: Map[String, AnyRef]
  )(
    implicit keyDeserializer: Deserializer[K],
    valueDeserializer:        Deserializer[V]
  ): F[KafkaConsumerServiceImpl[F, K, V]] = {
    for {
      kc <- Async[F].delay(new KafkaConsumer[K, V](kafkaConsumerConfig.asJava, keyDeserializer, valueDeserializer))
    } yield new KafkaConsumerServiceImpl[F, K, V](kc)
  }
}
