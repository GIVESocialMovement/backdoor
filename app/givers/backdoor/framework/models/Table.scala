package givers.backdoor.framework.models

import givers.backdoor.Permission
import play.api.libs.json.Json

case class AccessibleTable(
  base: TableModel,
  canRead: Boolean,
  canCreate: Boolean,
  canDelete: Boolean
) {
  def toJson = {
    base.toJson ++ Json.obj(
      "canRead" -> canRead,
      "canCreate" -> canCreate,
      "canDelete" -> canDelete
    )
  }
}

case class TableModel(
  name: String
) {
  private[models] def toJson = {
    Json.obj("name" -> name)
  }
}

