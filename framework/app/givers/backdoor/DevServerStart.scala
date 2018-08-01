package givers.backdoor

import play.api._
import play.core.server._

import scala.util.control.NonFatal

object DevServerStart {
  // Copy from https://github.com/playframework/playframework/blob/2.6.x/framework/src/play-server/src/main/scala/play/core/server/ProdServerStart.scala
  // Because we want to enable our users to start the server in development mode.
  def main(args: Array[String]): Unit = {
    val process = new RealServerProcess(args)
    start(process)
  }

  def start(process: ServerProcess): ReloadableServer = {
    try {

      val config = ProdServerStart.readServerConfigSettings(process).copy(mode = Mode.Dev)
      val pidFile = ProdServerStart.createPidFile(process, config.configuration)

      try {
        // Start the application
        val application: Application = {
          val environment = Environment(config.rootDir, process.classLoader, config.mode)
          val context = ApplicationLoader.createContext(environment)
          val loader = ApplicationLoader(context)
          loader.load(context)
        }
        Play.start(application)

        // Start the server
        val serverProvider: ServerProvider = ServerProvider.fromConfiguration(process.classLoader, config.configuration)
        val server = serverProvider.createServer(config, application)
        process.addShutdownHook {
          server.stop()
          pidFile.foreach(_.delete())
          assert(!pidFile.exists(_.exists), "PID file should not exist!")
        }
        server
      } catch {
        case NonFatal(e) =>
          // Clean up pidfile when the server fails to start
          pidFile.foreach(_.delete())
          throw e
      }
    } catch {
      case ServerStartException(message, cause) =>
        process.exit(message, cause)
      case NonFatal(e) =>
        process.exit("Oops, cannot start the server.", cause = Some(e))
    }
  }

}
