ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val common = (project in file("."))
  .settings(
    name := "common"
  )

val doobieVersion      = "0.13.4"
val circeVersion       = "0.14.1"
val circeConfigVersion = "0.8.0"
val monocleVersion     = "2.1.0"
val refinedVersion     = "0.9.28"
val enumeratumVersion  = "1.7.0"
val loggerVersion      = "2.17.2"
//
//ThisBuild / versionScheme := Some("early-semver")

libraryDependencies ++= Seq(
  "eu.timepit"                %% "refined"           % refinedVersion,
  "eu.timepit"                %% "refined-cats"      % refinedVersion,
  "org.tpolecat"              %% "doobie-core"       % doobieVersion,
  "org.tpolecat"              %% "doobie-postgres"   % doobieVersion,
  "org.tpolecat"              %% "doobie-specs2"     % doobieVersion,
  "org.tpolecat"              %% "doobie-refined"    % doobieVersion,
  "org.tpolecat"              %% "doobie-hikari"     % doobieVersion,
  "org.flywaydb"               % "flyway-core"       % "8.5.7",
  "org.slf4j"                  % "slf4j-nop"         % "1.7.36",
  "org.scalatest"             %% "scalatest"         % "3.2.11" % "test",
  "com.beachape"              %% "enumeratum-circe"  % enumeratumVersion,
  "com.beachape"              %% "enumeratum-doobie" % enumeratumVersion,
  "org.apache.logging.log4j"   % "log4j-api"         % loggerVersion,
  "org.apache.logging.log4j"   % "log4j-core"        % loggerVersion,
  "com.typesafe"               % "config"            % "1.4.2",
  "org.apache.kafka"           % "kafka-clients"     % "2.6.0",
  "com.github.pureconfig"     %% "pureconfig"        % "0.17.1",
  "com.typesafe"               % "config"            % "1.4.2",
  "com.fasterxml.jackson.core" % "jackson-databind"  % "2.13.2.2"
)

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
)

scalacOptions ++= Seq(
  "-Ymacro-annotations"
)
