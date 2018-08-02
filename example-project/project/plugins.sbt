resolvers ++= Seq(
  Resolver.bintrayRepo("givers", "maven"),
)

addSbtPlugin("givers.backdoor" % "sbt-backdoor" % "0.2.1")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.5")
