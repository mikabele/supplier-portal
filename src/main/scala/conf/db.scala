package conf

import cats.effect.{Async, Resource, Sync}
import cats.implicits._
import conf.app.DbConf
import doobie.Transactor
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.flywaydb.core.Flyway

// TODO - check how to migrate changes from sqk files if checksum was changed

object db {
  def transactor[F[_]: Async](
    dbConf: DbConf
  ): Resource[F, Transactor[F]] = for {
    ce <- ExecutionContexts.fixedThreadPool[F](10)
    tx <- HikariTransactor.newHikariTransactor[F](
      driverClassName = dbConf.driver,
      url             = dbConf.url,
      user            = dbConf.user,
      pass            = dbConf.password,
      connectEC       = ce
    )
  } yield tx

  class FlywayMigrator[F[_]: Sync](dbConf: DbConf) {
    def migrate(): F[Int] =
      for {
        conf <- migrationConfig(dbConf)
        res  <- Sync[F].delay(conf.migrate().migrationsExecuted)
      } yield res

    private def migrationConfig(dbConf: DbConf): F[Flyway] = {
      Sync[F].delay(
        Flyway
          .configure()
          .dataSource(dbConf.url, dbConf.user, dbConf.password)
          .locations(s"${dbConf.migrationLocation}/${dbConf.provider}")
          .load()
      )
    }
  }

  def migrator[F[_]: Sync](dbConf: DbConf): F[FlywayMigrator[F]] =
    new FlywayMigrator[F](dbConf).pure[F]
}