package givers.backdoor.framework.libraries

import play.api.i18n.Lang
import play.api.mvc.ControllerComponents

case class BadRequestException(message: String) extends Exception(message)
case class ForbiddenException(message: String) extends Exception(message)
case class NotFoundException(message: String = "") extends Exception(message)
case class ImpossibleException() extends Exception
case class RedirectException(url: String) extends Exception(url)
case class RequireLoginException() extends Exception("You are required to login before proceeding.")
case class ValidationException(key: String, args: Any*) extends Exception(key) {
  def getText(implicit cc: ControllerComponents, lang: Lang) = {
    cc.messagesApi(key, args:_*)
  }
}
