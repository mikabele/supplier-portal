package conf

object app {

  final case class AppConf(
    server: ServerConf,
    db:     DbConf,
    email:  EmailNotificatorConf
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
    host:             String,
    port:             Int,
    productTopicName: String
  )

  final case class EmailNotificatorConf(
    url:              String,
    email:            String,
    user:             String,
    password:         String,
    delay:            Int, //in minutes
    productTopicName: String,
    pollingTimeout:   Int //in seconds
  )
}
