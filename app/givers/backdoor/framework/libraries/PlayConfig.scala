package givers.backdoor.framework.libraries

import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, Environment, Mode}

@Singleton
class PlayConfig @Inject()(val config: Configuration, environment: Environment) {

  // There should be only one PlayConfig. Therefore,
  override def hashCode() = { 123 }
  override def equals(o: scala.Any) = { o.isInstanceOf[PlayConfig] }

  val APP_DOMAIN_WITH_PROTOCOL = getString("http.origin")

  val IS_PROD = environment.mode == Mode.Prod

  val SECRET = getString("play.http.secret.key")

  val SUPERVISED_DATABASE_URL = getString("target.databaseUrl")

  val AUTH0_CLIENT_ID = getString("auth0.clientId")
  val AUTH0_CLIENT_SECRET = getString("auth0.clientSecret")
  val AUTH0_DOMAIN = getString("auth0.domain")

  def getString(key: String): String = {
    getOptString(key).getOrElse {
      throw new Exception(s"The config '$key' doesn't exist in Play conf file or system properties")
    }
  }

  def getInt(key: String): Int = {
    getString(key).toInt
  }

  def getBoolean(key: String): Boolean = {
    getOptString(key).exists(_.toBoolean)
  }

  def getOptBoolean(key: String): Option[Boolean] = {
    getOptString(key).map(_.toBoolean)
  }

  def getOptString(key: String): Option[String] = {
    Option(System.getProperty(key)).orElse(config.getOptional[String](key))
  }
}
