package givers.backdoor.framework.services

import com.google.inject.{Inject, Singleton}
import givers.backdoor.framework.libraries.PlayConfig
import play.api.Logger
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}

object Auth0Service {
  case class User(
    email: String,
    isEmailVerified: Boolean
  )
}

@Singleton
class Auth0Service @Inject()(
  playConfig: PlayConfig,
  ws: WSClient
)(implicit ec: ExecutionContext) {

  import Auth0Service._

  val logger = Logger(this.getClass)

  def getAccessToken(code: String, callbackUrl: String): Future[String] = {
    ws.url(s"https://${playConfig.AUTH0_DOMAIN}/oauth/token")
        .withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
        .post(
          Json.obj(
            "client_id" -> playConfig.AUTH0_CLIENT_ID,
            "client_secret" -> playConfig.AUTH0_CLIENT_SECRET,
            "redirect_uri" -> callbackUrl,
            "code" -> code,
            "grant_type"-> "authorization_code"
          )
        )
      .map { response =>
        (response.json \ "access_token").as[String]
      }
  }

  def getUser(accessToken: String): Future[Option[Auth0Service.User]] = {
    ws.url(s"https://${playConfig.AUTH0_DOMAIN}/userinfo")
      .withQueryStringParameters("access_token" -> accessToken)
      .get()
      .map { resp =>
        (resp.json \ "email").asOpt[String].map { email =>
          User(
            email = email,
            isEmailVerified = (resp.json \ "email_verified").as[Boolean])
        }
      }
      .recover { case e: Exception =>
        logger.warn("Authorize user error", e)
        None
      }
  }
}
