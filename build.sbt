name := "supplier-portal"

version := "0.1"

scalaVersion := "2.13.6"

lazy val root = (project in file("."))
  .settings(
    name := "supplier-portal"
  )

val doobieVersion      = "0.13.4"
val http4sVersion      = "0.21.33"
val circeVersion       = "0.14.1"
val circeConfigVersion = "0.8.0"
val monocleVersion     = "2.1.0"
val refinedVersion     = "0.9.28"
val enumeratumVersion  = "1.7.0"
val loggerVersion      = "2.17.2"
val emailVersion       = "0.9.7"

libraryDependencies ++= Seq(
  "eu.timepit"              %% "refined"               % refinedVersion,
  "org.tpolecat"            %% "doobie-core"           % doobieVersion,
  "org.tpolecat"            %% "doobie-postgres"       % doobieVersion,
  "org.tpolecat"            %% "doobie-specs2"         % doobieVersion,
  "org.tpolecat"            %% "doobie-refined"        % doobieVersion,
  "org.tpolecat"            %% "doobie-hikari"         % doobieVersion,
  "org.tpolecat"            %% "doobie-postgres-circe" % doobieVersion,
  "org.http4s"              %% "http4s-dsl"            % http4sVersion,
  "org.http4s"              %% "http4s-blaze-server"   % http4sVersion,
  "org.http4s"              %% "http4s-blaze-client"   % http4sVersion,
  "org.http4s"              %% "http4s-circe"          % http4sVersion,
  "io.circe"                %% "circe-core"            % circeVersion,
  "io.circe"                %% "circe-generic"         % circeVersion,
  "io.circe"                %% "circe-generic-extras"  % circeVersion,
  "io.circe"                %% "circe-parser"          % circeVersion,
  "io.circe"                %% "circe-config"          % circeConfigVersion,
  "io.circe"                %% "circe-parser"          % circeVersion,
  "org.flywaydb"             % "flyway-core"           % "8.5.4",
  "org.scalatest"           %% "scalatest"             % "3.2.11" % "test",
  "com.beachape"            %% "enumeratum-circe"      % enumeratumVersion,
  "com.beachape"            %% "enumeratum-doobie"     % enumeratumVersion,
  "org.apache.logging.log4j" % "log4j-api"             % loggerVersion,
  "org.apache.logging.log4j" % "log4j-core"            % loggerVersion,
  "org.reactormonk"         %% "cryptobits"            % "1.3.1",
  "org.slf4j"                % "slf4j-nop"             % "1.7.36",
  "com.emarsys"             %% "scheduler"             % "0.4.5",
  "com.github.eikek"        %% "emil-common"           % emailVersion,
  "com.github.eikek"        %% "emil-javamail"         % emailVersion
)

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
)

scalacOptions ++= Seq(
  "-Ymacro-annotations"
)
