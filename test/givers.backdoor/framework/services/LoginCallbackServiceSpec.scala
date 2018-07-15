package givers.backdoor.framework.services

import givers.backdoor.framework.models.{LoginCallback, LoginCallbackTable}
import helpers.BaseSpec

class LoginCallbackServiceSpec extends BaseSpec {
  var service: LoginCallbackService = _

  before {
    resetDatabase()
    service = app.injector.instanceOf[LoginCallbackService]
  }

  it("creates, gets, and finish") {
    val login = await(service.create("origin"))

    login.status should be(LoginCallback.Status.Pending)
    login.originUrl should be("origin")

    await(service.getByState(s"${login.id}-${login.secretKey}abc")) should be(None) // wrong key.
    await(service.getByState(s"${login.id}-${login.secretKey}")) should be(Some(login))
    await(service.getByState(s"${login.id}-${login.secretKey}")) should be(None) // The login is already used.

    import dbConfig.profile.api._
    val query = TableQuery[LoginCallbackTable]
    val retrievedOpt = await(db.run {
      query.filter(_.id === login.id).take(1).result
    })

    retrievedOpt.map(_.id).headOption should be(Some(login.id))
    retrievedOpt.map(_.secretKey).headOption should not be Some(login.secretKey) // The key is hashed using Play's secret.
  }
}

