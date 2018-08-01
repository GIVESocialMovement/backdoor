name := "backdoor"
organization := "givers"

lazy val framework = RootProject(file("framework"))
lazy val sbtPlugin = RootProject(file("sbt-plugin"))

lazy val root = project.in(file(".")).aggregate(framework, sbtPlugin)

publish := {}
publishM2 := {}
publishLocal := {}
