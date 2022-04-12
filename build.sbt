ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "supplier-portal"
  )

val doobieVersion      = "1.0.0-RC1"
val http4sVersion      = "0.23.11"
val circeVersion       = "0.14.1"
val circeConfigVersion = "0.8.0"
val monocleVersion     = "2.1.0"
val refinedVersion     = "0.9.28"
val enumeratumVersion  = "1.7.0"
val loggerVersion      = "2.17.2"

libraryDependencies ++= Seq(
  "eu.timepit"                 %% "refined"                % refinedVersion,
  "eu.timepit"                 %% "refined-cats"           % refinedVersion,
  "org.typelevel"              %% "cats-effect"            % "3.3.8",
  "org.tpolecat"               %% "doobie-core"            % doobieVersion,
  "org.tpolecat"               %% "doobie-postgres"        % doobieVersion,
  "org.tpolecat"               %% "doobie-specs2"          % doobieVersion,
  "org.tpolecat"               %% "doobie-refined"         % doobieVersion,
  "org.tpolecat"               %% "doobie-hikari"          % doobieVersion,
  "org.tpolecat"               %% "doobie-postgres-circe"  % "0.13.4",
  "org.http4s"                 %% "http4s-dsl"             % http4sVersion,
  "org.http4s"                 %% "http4s-blaze-server"    % http4sVersion,
  "org.http4s"                 %% "http4s-blaze-client"    % http4sVersion,
  "org.http4s"                 %% "http4s-circe"           % http4sVersion,
  "org.http4s"                 %% "http4s-jdk-http-client" % "0.7.0",
  "io.circe"                   %% "circe-core"             % circeVersion,
  "io.circe"                   %% "circe-generic"          % circeVersion,
  "io.circe"                   %% "circe-generic-extras"   % circeVersion,
  "io.circe"                   %% "circe-optics"           % circeVersion,
  "io.circe"                   %% "circe-parser"           % circeVersion,
  "io.circe"                   %% "circe-config"           % circeConfigVersion,
  "io.circe"                   %% "circe-core"             % circeVersion,
  "io.circe"                   %% "circe-generic"          % circeVersion,
  "io.circe"                   %% "circe-generic-extras"   % circeVersion,
  "io.circe"                   %% "circe-optics"           % circeVersion,
  "io.circe"                   %% "circe-parser"           % circeVersion,
  "io.circe"                   %% "circe-refined"          % circeVersion,
  "org.flywaydb"                % "flyway-core"            % "8.5.4",
  "org.slf4j"                   % "slf4j-nop"              % "1.7.36",
  "com.github.julien-truffaut" %% "monocle-core"           % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-refined"        % monocleVersion,
  "com.github.julien-truffaut" %% "monocle-macro"          % monocleVersion,
  "org.scalatest"              %% "scalatest"              % "3.2.11" % "test",
  "com.beachape"               %% "enumeratum-circe"       % enumeratumVersion,
  "com.beachape"               %% "enumeratum-doobie"      % enumeratumVersion,
  "org.apache.logging.log4j"    % "log4j-api"              % loggerVersion,
  "org.apache.logging.log4j"    % "log4j-core"             % loggerVersion,
  "org.reactormonk"            %% "cryptobits"             % "1.3.1"
)

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
)
