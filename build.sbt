name := "backdoor"
organization := "givers"

lazy val framework = RootProject(file("framework-backdoor"))
lazy val sbtPlugin = RootProject(file("sbt-backdoor"))

lazy val root = project.in(file(".")).aggregate(framework, sbtPlugin)

publish := {}
publishM2 := {}
publishLocal := {}
