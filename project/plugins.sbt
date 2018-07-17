resolvers ++= Seq(
  Classpaths.sbtPluginReleases,
  Resolver.bintrayRepo("givers", "maven")
)

addSbtPlugin("givers.vuefy" % "sbt-vuefy" % "1.0.0")
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.6.16")
addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")
addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")
addSbtPlugin("org.irundaia.sbt" % "sbt-sassify" % "1.4.12")
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.5.1")
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.4")
