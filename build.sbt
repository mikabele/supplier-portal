ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "supplier-portal"
  )

scalacOptions += "-Ypartial-unification"

libraryDependencies ++= Seq(
  "eu.timepit"    %% "refined"     % "0.9.28",
  "org.typelevel" %% "cats-effect" % "3.3.7"
)
