package givers.backdoor.framework.libraries

import com.google.inject.Inject
import givers.backdoor.framework.services.{AccessService, AccessibleTableService, UserService}
import play.api.mvc.MessagesControllerComponents

import scala.concurrent.ExecutionContext

class ControllerContext @Inject()(
  implicit val ec: ExecutionContext,
  val mcc: MessagesControllerComponents,
  val tableService: AccessibleTableService,
  val config: PlayConfig,
  val userService: UserService,
  val accessService: AccessService
)
