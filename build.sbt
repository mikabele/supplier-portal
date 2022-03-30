ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "supplier-portal"
  )

scalacOptions += "-Ypartial-unification"

val doobieVersion           = "1.0.0-RC1"
val http4sVersion           = "0.23.11"
val circeVersion            = "0.14.1"
val circeConfigVersion      = "0.8.0"
val dtoMapperChimneyVersion = "0.6.1"

libraryDependencies ++= Seq(
  "eu.timepit"    %% "refined"                % "0.9.28",
  "org.typelevel" %% "cats-effect"            % "3.3.8",
  "org.tpolecat"  %% "doobie-core"            % doobieVersion,
  "org.tpolecat"  %% "doobie-postgres"        % doobieVersion,
  "org.tpolecat"  %% "doobie-specs2"          % doobieVersion,
  "org.http4s"    %% "http4s-dsl"             % http4sVersion,
  "org.http4s"    %% "http4s-blaze-server"    % http4sVersion,
  "org.http4s"    %% "http4s-blaze-client"    % http4sVersion,
  "org.http4s"    %% "http4s-circe"           % http4sVersion,
  "org.http4s"    %% "http4s-jdk-http-client" % "0.7.0",
  "io.circe"      %% "circe-core"             % circeVersion,
  "io.circe"      %% "circe-generic"          % circeVersion,
  "io.circe"      %% "circe-generic-extras"   % circeVersion,
  "io.circe"      %% "circe-optics"           % circeVersion,
  "io.circe"      %% "circe-parser"           % circeVersion,
  "io.circe"      %% "circe-config"           % circeConfigVersion,
  "io.circe"      %% "circe-core"             % circeVersion,
  "io.circe"      %% "circe-generic"          % circeVersion,
  "io.circe"      %% "circe-generic-extras"   % circeVersion,
  "io.circe"      %% "circe-optics"           % circeVersion,
  "io.circe"      %% "circe-parser"           % circeVersion,
  "io.scalaland"  %% "chimney"                % dtoMapperChimneyVersion,
)

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
)
