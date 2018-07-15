package givers.backdoor.framework.controllers

import givers.backdoor.framework.libraries._
import givers.backdoor.framework.models.AccessibleTable
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json._
import play.api.libs.json.Writes._
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class BaseController()(
  implicit cc: ControllerContext,
  config: PlayConfig
) extends MessagesAbstractController(cc.mcc) with I18nSupport {

  private[this] val logger = Logger(this.getClass)

  implicit def controllerContext2ExecutionContext(implicit cc: ControllerContext): ExecutionContext = cc.ec
  implicit def controllerContext2ControllerComponents(implicit cc: ControllerContext): ControllerComponents = cc.mcc
  implicit def context2Request[R](implicit context: BaseContext[R]): Request[R] = context.request
  implicit def context2Lang[R](implicit context: BaseContext[R]): play.api.i18n.Lang = context.request.lang

  def async(fn: Context[AnyContent] => Future[Result]): Action[AnyContent] = {
    async(parse.anyContent)(fn)
  }

  /* A convenient method for handling form submission.
   * All of our form submissions should be through POST AJAX.
   * The backend always sends a JSON back with success=[true|false].
   */
  protected[this] def handleForm[T](
    form: Form[T],
    errorKeyPrefix: String
  )(
    fn: T => Future[Result]
  )(
    implicit content: BaseContext[_]
  ): Future[Result] = {
    form.bindFromRequest.fold(
      hasErrors = { error =>
        Future(Ok(toJson(obj(
          "success" -> false,
          "errors" -> error.errors.map { e =>
            Map(
              "key" -> e.key,
              "message" -> messagesApi(s"$errorKeyPrefix.${e.key}.${e.message}")
            )
          }
        ))))
      },
      success = fn
    )
  }

  def async[R](
    bodyParser: BodyParser[R]
  )(
    fn: Context[R] => Future[Result]
  ): Action[R] = Action.async(bodyParser) { implicit request =>
    cc.userService.getUserFromCookies(request.cookies)
      .flatMap { userOpt =>
        implicit val context = Context(
          loggedInUserOpt = userOpt,
          request = request,
          config = cc.config)

        Future.unit
          // This guarantees that all thrown errors are caught.
          .flatMap { _ => fn(context) }
          .recover {
            case _: RequireLoginException => Ok(givers.backdoor.framework.views.html.login())
            case e: ForbiddenException => Ok(givers.backdoor.framework.views.html.forbidden(e.message))
            case e: Exception =>
              println(e)
              if (request.acceptedTypes.exists(_.mediaSubType == "json")) {
                BadRequest(toJson(obj(
                  "success" -> false,
                  "errors" -> Seq(obj(
                    "message" -> e.getMessage,
                    "trace" -> e.getStackTrace.mkString("\n"),
                    "cause" -> Option(e.getCause).map(_.getMessage)
                  ))
                )))
              } else {
                BadRequest(e.getMessage)
              }
          }
      }
  }

  def asyncAuthenticated(fn: AuthenticatedContext[AnyContent] => Future[Result]): Action[AnyContent] = {
    asyncAuthenticated(parse.anyContent)(fn)
  }

  def asyncAuthenticated[R](
    bodyParser: BodyParser[R]
  )(
    fn: AuthenticatedContext[R] => Future[Result]
  ) = async(bodyParser) { implicit context =>
    context.loggedInUserOpt.map { loggedInUser =>
      if (!cc.accessService.hasAccess(loggedInUser)) {
        throw ForbiddenException("You are not allowed to access this page.")
      }
      fn(AuthenticatedContext(
        loggedInUser = loggedInUser,
        request = context.request,
        config = context.config,
        page = context.page
      ))
    }.getOrElse {
      throw RequireLoginException()
    }
  }

  def asyncAuthenticatedTable[R](
    table: String,
    bodyParser: BodyParser[R]
  )(
    fn: AccessibleTable => AuthenticatedContext[R] => Future[Result]
  ): Action[R] = asyncAuthenticated(bodyParser) { implicit context =>
    cc.tableService.getTable(table)
      .map {
        _.getOrElse { throw NotFoundException() }
      }
      .flatMap { table => fn(table)(context) }
  }
}
