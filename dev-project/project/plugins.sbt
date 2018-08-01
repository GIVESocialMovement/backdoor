lazy val root = project.in(file(".")).dependsOn(PlayBackdoor).aggregate(PlayBackdoor)
lazy val PlayBackdoor = RootProject(file("../../sbt-plugin"))
addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.3.5")