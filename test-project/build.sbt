name := "app"
organization := "givers.backdoor.testproject"
version := "1.0-SNAPSHOT"

lazy val root = Project("app", file(".")).enablePlugins(JavaAppPackaging)

resolvers ++= Seq(
  Resolver.bintrayRepo("givers", "maven"),
)

libraryDependencies ++= Seq(
//  "givers" %% "backdoor" % "0.1.7",
//  "givers" %% "backdoor" % "0.1.7" classifier "assets",
)

run / javaOptions += "-Dconfig.resource=dev.conf"
run / fork := true

mainClass in (Compile, Keys.run) := Some("givers.backdoor.DevServerStart")
mainClass in Compile := Some("play.core.server.ProdServerStart")

// For local development
val artifactVersion = "0.1.8-SNAPSHOT"

unmanagedJars in Compile ++= Seq(
  file(s"../target/scala-2.12/backdoor_2.12-$artifactVersion.jar"),
  file(s"../target/scala-2.12/backdoor_2.12-$artifactVersion-web-assets.jar"),
)

externalPom(Def.setting(file(s"../target/scala-2.12/backdoor_2.12-$artifactVersion.pom")))

