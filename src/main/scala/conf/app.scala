package conf

import io.circe.generic.JsonCodec

object app {

  final case class AppConf(
    server: ServerConf,
    db:     DbConf
  )

  final case class DbConf(
    provider:          String,
    driver:            String,
    url:               String,
    user:              String,
    password:          String,
    migrationLocation: String
  )

  final case class ServerConf(
    host: String,
    port: Int
  )

}
