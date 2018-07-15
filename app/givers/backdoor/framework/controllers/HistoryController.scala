package givers.backdoor.framework.controllers

import com.google.inject.{Inject, Singleton}
import givers.backdoor.framework.libraries.{ControllerContext, PlayConfig}
import givers.backdoor.framework.services.HistoryEntryService


object HistoryController {
  val PAGE_SIZE = 100
}

@Singleton
class HistoryController @Inject()(
  historyEntryService: HistoryEntryService
)(
  implicit cc: ControllerContext,
  config: PlayConfig
) extends BaseController() {

  import HistoryController._

  def index(exclusiveMaxIdOpt: Option[Long]) = asyncAuthenticated { implicit context =>
    historyEntryService.getRiches(exclusiveMaxIdOpt, PAGE_SIZE).map { items =>
      Ok(givers.backdoor.framework.views.html.history(items))
    }
  }

}
