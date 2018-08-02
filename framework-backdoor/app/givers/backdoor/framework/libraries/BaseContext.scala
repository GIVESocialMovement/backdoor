package givers.backdoor.framework.libraries

import givers.backdoor.framework.models.RichUser
import play.api.mvc.MessagesRequest
import play.filters.csrf.CSRF

sealed abstract class BaseContext[R] {
  def request: MessagesRequest[R]
  def config: PlayConfig
  def page: PageContext

  def loggedInUserOpt: Option[RichUser]

  def getCsrfToken = CSRF.getToken(request).map(_.value).getOrElse("")
}

case class AuthenticatedContext[R](
  loggedInUser: RichUser,
  request: MessagesRequest[R],
  config: PlayConfig,
  page: PageContext = PageContext()
) extends BaseContext[R] {
  val user = loggedInUser
  def loggedInUserOpt: Option[RichUser] = Some(loggedInUser)

  def setTitle(title: String) = this.copy(page = page.copy(title = title))
}

case class Context[R](
  loggedInUserOpt: Option[RichUser],
  request: MessagesRequest[R],
  config: PlayConfig,
  page: PageContext = PageContext()
) extends BaseContext[R] {

  def setTitle(title: String) = this.copy(page = page.copy(title = title))
}
