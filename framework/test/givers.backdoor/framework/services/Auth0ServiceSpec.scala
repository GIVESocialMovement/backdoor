package givers.backdoor.framework.services

import helpers.{BaseSpec, Maker}
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.http.{HeaderNames, MimeTypes}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.{WSClient, WSRequest}

import scala.concurrent.Future

class Auth0ServiceSpec extends BaseSpec {
  var service: Auth0Service = _
  var ws: WSClient = _

  before {
    ws = mock[WSClient]
    service = new Auth0Service(config, ws)
  }

  it("gets access token") {
    val req = mock[WSRequest]
    when(req.withHttpHeaders(any())).thenReturn(req)
    when(req.post(any[JsValue]())(any())).thenReturn(Future(Maker.wsResponse(200, Json.obj("access_token" -> "tokenValue"))))
    when(ws.url(any())).thenReturn(req)

    await(service.getAccessToken("testCode", "callbackUrl")) should be("tokenValue")

    verify(ws).url(s"https://${config.AUTH0_DOMAIN}/oauth/token")
    verify(req).post(
      Json.obj(
        "client_id" -> config.AUTH0_CLIENT_ID,
        "client_secret" -> config.AUTH0_CLIENT_SECRET,
        "redirect_uri" -> "callbackUrl",
        "code" -> "testCode",
        "grant_type"-> "authorization_code"
      )
    )
    verify(req).withHttpHeaders(HeaderNames.ACCEPT -> MimeTypes.JSON)
  }

  it("gets email") {
    val req = mock[WSRequest]
    when(req.withQueryStringParameters(any())).thenReturn(req)
    when(req.get()).thenReturn(Future(Maker.wsResponse(200, Json.obj("email" -> "test@give.asia", "email_verified" -> true))))
    when(ws.url(any())).thenReturn(req)

    await(service.getUser("tokenValue")) should be(Some(Auth0Service.User(email = "test@give.asia", isEmailVerified = true)))

    verify(ws).url(s"https://${config.AUTH0_DOMAIN}/userinfo")
    verify(req).get()
    verify(req).withQueryStringParameters("access_token" -> "tokenValue")
  }
}
