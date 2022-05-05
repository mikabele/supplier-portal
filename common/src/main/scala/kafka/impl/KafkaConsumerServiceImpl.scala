package kafka.impl

import cats.Monad
import cats.effect.Async
import cats.syntax.all._
import kafka.KafkaConsumerService
import org.apache.kafka.clients.consumer.{ConsumerRecord, KafkaConsumer}

import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._
import scala.jdk.DurationConverters._

class KafkaConsumerServiceImpl[F[_]: Monad: Async, K, V](
  kafkaConsumer: KafkaConsumer[K, V]
) extends KafkaConsumerService[F, K, V] {
  override def poll(topic: String, pollingTimeout: FiniteDuration): F[List[ConsumerRecord[K, V]]] = {
    for {
      records <- Async[F].delay(kafkaConsumer.poll(pollingTimeout.toJava))
    } yield records.records(topic).asScala.toList
  }

  override def subscribe(topics: List[String]): F[Unit] = {
    for {
      _ <- Async[F].delay(kafkaConsumer.subscribe(topics.asJavaCollection))
    } yield ()
  }
}
