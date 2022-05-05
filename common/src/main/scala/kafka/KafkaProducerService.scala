package kafka

import cats.effect.Async
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
  ): KafkaProducerService[F, K, V] = {
    val kp = new KafkaProducer(kafkaProducerConfig.asJava, keySerializer, valueSerializer)
    new KafkaProducerServiceImpl[F, K, V](kp, topic)
  }
}
