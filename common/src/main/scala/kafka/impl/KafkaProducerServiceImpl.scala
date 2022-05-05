package kafka.impl

import cats.effect.Async
import kafka.KafkaProducerService
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

class KafkaProducerServiceImpl[F[_]: Async, K, V](kafkaProducer: KafkaProducer[K, V], topic: String)
  extends KafkaProducerService[F, K, V] {
  override def send(key: K, value: V): F[Unit] = {
    val producerRecord = new ProducerRecord[K, V](topic, key, value)
    Async[F].delay(kafkaProducer.send(producerRecord))
  }
}
