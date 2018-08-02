name := "dev-project"
organization := "givers.backdoor"
version := "1.0-SNAPSHOT"

resolvers ++= Seq(
  Resolver.bintrayRepo("givers", "maven"),
  Resolver.mavenLocal
)

lazy val root = project.in(file("."))
  .enablePlugins(Backdoor, JavaAppPackaging)
  .dependsOn(BackdoorFramework)
  .aggregate(BackdoorFramework)

lazy val BackdoorFramework = RootProject(file("../framework-backdoor"))

