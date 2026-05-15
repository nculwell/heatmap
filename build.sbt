ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.4"
ThisBuild / organization := "heatmap"

lazy val root = (project in file("."))
  .settings(
    name := "heatmap",
    Compile / run / fork := true,
  )
