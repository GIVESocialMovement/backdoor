resolvers ++= Seq(
  Resolver.bintrayRepo("givers", "maven"),
  Resolver.mavenLocal
)

addSbtPlugin("givers.backdoor" % "sbt-plugin" % "0.1.8-SNAPSHOT")
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.5")
