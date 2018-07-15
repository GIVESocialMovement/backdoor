name := "backdoor"
organization := "givers"
version := "0.1.4"

lazy val root = (project in file(".")).enablePlugins(PlayScala, SbtWeb, SbtVuefy)

scalaVersion := "2.12.6"

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings",                  // Fail compilation on warnings. We shouldn't have any warning.
)

libraryDependencies ++= Seq(
  ws,
  filters,
  guice,
  "com.github.tminglei" %% "slick-pg" % "0.15.3" withSources() withJavadoc(),
  "com.github.tminglei" %% "slick-pg_play-json" % "0.15.3" withSources() withJavadoc(),
  "com.twitter" %% "util-collection" % "7.0.0" withSources() withJavadoc(),
  "com.typesafe.play" %% "play-slick" % "3.0.0" withSources() withJavadoc(),
  "com.typesafe.play" %% "play-slick-evolutions" % "3.0.0" withSources() withJavadoc(),
  "org.mindrot"  % "jbcrypt"   % "0.4" withSources() withJavadoc(),
  "org.postgresql" % "postgresql" % "42.1.4" withSources() withJavadoc(),
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.0.0" % Test withSources() withJavadoc(),
  "org.mockito" % "mockito-core" % "1.10.19" % Test withSources() withJavadoc()
)

// Adds additional packages into Twirl
TwirlKeys.templateImports ++= Seq(
  "play.api.libs.json.Json._",
  "givers.backdoor.framework.libraries._",
  "givers.backdoor.framework._"
)

testOptions in Test += Tests.Argument("-oF")

excludeFilter in digest := "*.vue"
excludeFilter in gzip := "*.vue"

Assets / VueKeys.vuefy / VueKeys.prodCommands := Set("stage")
Assets / VueKeys.vuefy / VueKeys.webpackBinary := "./node_modules/.bin/webpack"
Assets / VueKeys.vuefy / VueKeys.webpackConfig := "./webpack.config.js"

pipelineStages := Seq(
  digest,
  gzip)

javaOptions ++= Seq("-Xmx2G")

// I'm not sure why we need it. But it suppresses sbt.TrapExitSecurityException when running
// `sbt -mem 2048 -Dconfig.file=conf/application.conf run-main scripts.AlgoliaScript`. This also corrects the
// exit code. See: https://stackoverflow.com/questions/21464673/sbt-trapexitsecurityexception-thrown-at-sbt-run
trapExit := false

cancelable in Global := true

publishMavenStyle := true
bintrayOrganization := Some("givers")
bintrayRepository := "maven"
publishArtifact in Test := false
pomIncludeRepository := { _ => false }
licenses := Seq(("MIT", url("http://opensource.org/licenses/MIT")))

packagedArtifacts in publish := {
  val artifacts: Map[sbt.Artifact, java.io.File] = (packagedArtifacts in publishLocal).value
  val assets: java.io.File = (PlayKeys.playPackageAssets in Compile).value
  artifacts + (Artifact(moduleName.value, "asset", "jar", "assets") -> assets)
}

packagedArtifacts in publishM2 := {
  val artifacts: Map[sbt.Artifact, java.io.File] = (packagedArtifacts in publishLocal).value
  val assets: java.io.File = (PlayKeys.playPackageAssets in Compile).value
  artifacts + (Artifact(moduleName.value, "asset", "jar", "assets") -> assets)
}
