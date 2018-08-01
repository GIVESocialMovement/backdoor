name := "example-project"
organization := "givers.backdoor"
version := "1.0-SNAPSHOT"

lazy val root = project.in(file(".")).enablePlugins(Backdoor, JavaAppPackaging)
