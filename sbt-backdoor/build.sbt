name := "sbt-backdoor"
organization := "givers.backdoor"
version := "0.2.2"

sbtPlugin := true
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.16")

publishMavenStyle := true
bintrayOrganization := Some("givers")
bintrayRepository := "maven"
publishArtifact in Test := false
pomIncludeRepository := { _ => false }
licenses := Seq(("MIT", url("http://opensource.org/licenses/MIT")))
