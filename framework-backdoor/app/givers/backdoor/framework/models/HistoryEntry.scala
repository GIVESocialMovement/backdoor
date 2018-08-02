package givers.backdoor.framework.models

import com.twitter.util.Time
import play.api.libs.json.{JsValue, Json}
import givers.backdoor.framework.libraries.PostgresProfile.api._

case class HistoryEntry(
  id: Long,
  userId: Long,
  table: String,
  originalRecord: JsValue,
  sql: String,
  performedAt: Time
) {
  def toJson = {
    Json.obj(
      "id" -> id,
      "userId" -> userId,
      "table" -> table,
      "sql" -> sql,
      "performedAt" -> performedAt.inSeconds
    )
  }
}

case class RichHistoryEntry(
  base: HistoryEntry,
  user: User
) {
  def toJson = {
    base.toJson ++ Json.obj(
      "user" -> user.toJson
    )
  }
}

class HistoryEntryTable(tag: Tag) extends Table[HistoryEntry](tag, "history_entries") {

  import givers.backdoor.framework.libraries.ExtraPostgresColumnType._

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def userId = column[Long]("user_id")
  def table = column[String]("table")
  def originalRecord = column[JsValue]("original_record")
  def sql = column[String]("sql")
  def performedAt = column[Time]("performed_at")

  def * = (id, userId, table, originalRecord, sql, performedAt) <> ((HistoryEntry.apply _).tupled, HistoryEntry.unapply)
}
