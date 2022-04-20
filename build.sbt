ThisBuild / name := "supplier-portal"

ThisBuild / version := "0.1"

scalaVersion := "2.13.6"

val doobieVersion      = "0.13.4"
val http4sVersion      = "0.21.33"
val circeVersion       = "0.14.1"
val circeConfigVersion = "0.8.0"
val refinedVersion     = "0.9.28"
val loggerVersion      = "2.17.2"

lazy val common = Project(id = "common", base = file("common"))

lazy val root = Project(id = "root", base = file("."))
  .dependsOn(common)

libraryDependencies ++= Seq(
  "org.http4s"              %% "http4s-dsl"           % http4sVersion,
  "org.http4s"              %% "http4s-blaze-server"  % http4sVersion,
  "org.http4s"              %% "http4s-blaze-client"  % http4sVersion,
  "org.http4s"              %% "http4s-circe"         % http4sVersion,
  "io.circe"                %% "circe-core"           % circeVersion,
  "io.circe"                %% "circe-generic"        % circeVersion,
  "io.circe"                %% "circe-generic-extras" % circeVersion,
  "io.circe"                %% "circe-optics"         % circeVersion,
  "io.circe"                %% "circe-parser"         % circeVersion,
  "io.circe"                %% "circe-config"         % circeConfigVersion,
  "io.circe"                %% "circe-core"           % circeVersion,
  "io.circe"                %% "circe-generic"        % circeVersion,
  "io.circe"                %% "circe-generic-extras" % circeVersion,
  "io.circe"                %% "circe-optics"         % circeVersion,
  "io.circe"                %% "circe-parser"         % circeVersion,
  "io.circe"                %% "circe-refined"        % circeVersion,
  "org.slf4j"                % "slf4j-nop"            % "1.7.36",
  "org.scalatest"           %% "scalatest"            % "3.2.11" % "test",
  "org.apache.logging.log4j" % "log4j-api"            % loggerVersion,
  "org.apache.logging.log4j" % "log4j-core"           % loggerVersion,
  "org.reactormonk"         %% "cryptobits"           % "1.3.1"
)

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
)

scalacOptions ++= Seq(
  "-Ymacro-annotations"
)
