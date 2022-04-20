ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

val loggerVersion      = "2.17.2"
val emailVersion       = "0.9.7"
val circeVersion       = "0.14.1"
val circeConfigVersion = "0.8.0"
val doobieVersion      = "0.13.4"

lazy val common = RootProject(file("../common"))

lazy val root = Project(id = "scheduler", base = file(".")).dependsOn(common)

libraryDependencies ++= Seq(
  "org.scalatest"           %% "scalatest"            % "3.2.11" % "test",
  "org.apache.logging.log4j" % "log4j-api"            % loggerVersion,
  "org.apache.logging.log4j" % "log4j-core"           % loggerVersion,
  "org.slf4j"                % "slf4j-nop"            % "1.7.36",
  "com.emarsys"             %% "scheduler"            % "0.4.5",
  "com.github.eikek"        %% "emil-common"          % emailVersion,
  "com.github.eikek"        %% "emil-javamail"        % emailVersion,
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
  "org.tpolecat"            %% "doobie-postgres"      % doobieVersion,
  "org.tpolecat"            %% "doobie-specs2"        % doobieVersion,
  "org.tpolecat"            %% "doobie-refined"       % doobieVersion,
  "org.tpolecat"            %% "doobie-hikari"        % doobieVersion
)

addCompilerPlugin(
  "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
)

scalacOptions ++= Seq(
  "-Ymacro-annotations",
  "-Ylog-classpath"
)
