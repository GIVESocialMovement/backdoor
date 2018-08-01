package services

import givers.backdoor.framework.models.User
import givers.backdoor.framework.services.UserService
import helpers.BaseSpec
import play.api.mvc.{Cookie, Cookies}

class UserServiceSpec extends BaseSpec {
  var service: UserService = _

  before {
    resetDatabase()
    service = app.injector.instanceOf[UserService]
  }

  it("creates") {
    val user = await(service.create("TEST@give.asia  "))
    user.email should be("test@give.asia")
  }

  it("gets from cookie") {
    val user = await(service.create("test@give.asia"))

    val checksum = UserService.generateChecksum(user.id, user.cookieSecret, config.SECRET)

    await(service.getUserFromCookies(Cookies(Seq(Cookie(User.COOKIE_KEY_ID, user.id.toString), Cookie(User.COOKIE_KEY_SECRET, user.cookieSecret))))) should be(None) // the secret isn't hashed
    await(service.getUserFromCookies(Cookies(Seq(Cookie(User.COOKIE_KEY_ID, user.id.toString), Cookie(User.COOKIE_KEY_SECRET, s"aaa$checksum"))))) should be(None) // the hash is wrong.
    await(service.getUserFromCookies(Cookies(Seq(Cookie(User.COOKIE_KEY_ID, (user.id + 1L).toString), Cookie(User.COOKIE_KEY_SECRET, checksum))))) should be(None) // the user id is wrong.
    await(service.getUserFromCookies(Cookies(Seq(Cookie(User.COOKIE_KEY_ID, user.id.toString), Cookie(User.COOKIE_KEY_SECRET, checksum))))) should be(Some(user))
  }
}
