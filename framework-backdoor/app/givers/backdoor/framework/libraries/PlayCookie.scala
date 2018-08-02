package givers.backdoor.framework.libraries

import play.api.mvc.Cookie
import views.html.helper

import scala.language.implicitConversions

object PlayCookie {
  implicit def cookie2PlayCookie(cookie: Cookie): PlayCookie = {
    PlayCookie(
      name = cookie.name,
      decodedValue = Helper.urlDecode(cookie.value),
      secure = cookie.secure
    )
  }

  implicit def playCookie2Cookie(cookie: PlayCookie): Cookie = cookie.toCookie
}

case class PlayCookie(
  name: String,
  decodedValue: String,
  secure: Boolean
) {
  def toCookie: Cookie = {
    Cookie(
      name = name,
      value = helper.urlEncode(decodedValue),
      secure = secure
    )
  }
}
