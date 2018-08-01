package givers.backdoor.sbtplugin

import play.sbt.PlayImport.PlayKeys
import play.sbt.{PlayImport, PlayScala}
import sbt.AutoPlugin
import sbt._

object Backdoor extends AutoPlugin {
  override def requires = PlayScala
  override def projectSettings = PlayScala.projectSettings ++ Seq(
    Keys.libraryDependencies ++= Seq(
      PlayImport.guice,
      "givers.backdoor" %% "framework" % "0.1.8-SNAPSHOT"
    ),
    PlayKeys.playDefaultPort := 8000
  )
}

// A development plugin doesn't include the framework.
object DevBackdoor extends AutoPlugin {
  override def requires = PlayScala
  override def projectSettings = PlayScala.projectSettings ++ Seq(
    Keys.libraryDependencies ++= Seq(
      PlayImport.guice
    ),
    PlayKeys.playDefaultPort := 8000
  )
}
