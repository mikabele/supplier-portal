package util

import org.apache.kafka.common.serialization.{Deserializer, Serializer}

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

//TODO - use circe-kafka

object KafkaSerializationUtil {
  implicit def kafkaSerializer[V]: Serializer[V] = (_: String, data: V) => {
    val stream: ByteArrayOutputStream = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(stream)
    oos.writeObject(data)
    oos.close()
    stream.toByteArray
  }

  implicit def kafkaDeserializer[V]: Deserializer[V] = (_: String, data: Array[Byte]) => {
    val ois   = new ObjectInputStream(new ByteArrayInputStream(data))
    val value = ois.readObject.asInstanceOf[V]
    ois.close()
    value
  }
}
