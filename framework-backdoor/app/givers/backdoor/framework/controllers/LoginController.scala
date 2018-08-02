package givers.backdoor.framework.controllers

import java.net.URLEncoder

import com.google.inject.{Inject, Singleton}
import com.twitter.conversions.time._
import givers.backdoor.framework.libraries.{ControllerContext, PlayConfig}
import givers.backdoor.framework.models.User
import givers.backdoor.framework.services.{Auth0Service, LoginCallbackService, UserService}
import play.api.mvc.{Cookie, DiscardingCookie}

import scala.concurrent.Future

@Singleton
class LoginController @Inject()(
  auth0Service: Auth0Service,
  loginCallbackService: LoginCallbackService,
  userService: UserService,
)(
  implicit cc: ControllerContext,
  config: PlayConfig
) extends BaseController() {

  val redirectUri = s"${cc.config.APP_DOMAIN_WITH_PROTOCOL}${routes.LoginController.callback(None, None).url}"

  def login(originOpt: Option[String]) = async { implicit context =>
    loginCallbackService.create(originOpt.getOrElse("/")).map { loginCallback =>
      Redirect(
        s"https://${cc.config.AUTH0_DOMAIN}/authorize?client_id=${cc.config.AUTH0_CLIENT_ID}&redirect_uri=${URLEncoder.encode(redirectUri, "UTF-8")}&response_type=code&scope=openid profile email&state=${URLEncoder.encode(loginCallback.state, "UTF-8")}"
      )
    }
  }

  def callback(codeOpt: Option[String], stateOpt: Option[String]) = async { implicit context =>
    val code = codeOpt.get
    val state = stateOpt.get

    for {
      login <- loginCallbackService.getByState(state).map(_.get)
      accessToken <- auth0Service.getAccessToken(code, redirectUri)
      email <- auth0Service.getUser(accessToken)
        .map(_.get)
        .map { resp =>
          if (!resp.isEmailVerified) {
            throw new Exception("The email isn't verified.")
          }

          resp.email
        }
      user <- userService.getByEmail(email)
          .flatMap {
            case Some(user) => Future(user)
            case None => userService.create(email)
          }

    } yield {
      Redirect(login.originUrl)
        .withCookies(
          Cookie(User.COOKIE_KEY_ID, user.id.toString, maxAge = Some(7.days.inSeconds), secure = config.IS_PROD),
          Cookie(User.COOKIE_KEY_SECRET, UserService.generateChecksum(user.id, user.cookieSecret, config.SECRET), maxAge = Some(7.days.inSeconds), secure = config.IS_PROD)
        )
    }
  }

  def logout = async { implicit context =>
    Future {
      Redirect(
          s"https://${cc.config.AUTH0_DOMAIN}/logout?client_id=${cc.config.AUTH0_CLIENT_ID}&returnTo=${URLEncoder.encode(cc.config.APP_DOMAIN_WITH_PROTOCOL, "UTF-8")}"
        )
        .discardingCookies(
          DiscardingCookie(User.COOKIE_KEY_ID),
          DiscardingCookie(User.COOKIE_KEY_SECRET)
        )
    }
  }
}
