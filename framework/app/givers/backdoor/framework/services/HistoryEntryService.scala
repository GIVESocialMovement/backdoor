package givers.backdoor.framework.services

import java.sql.PreparedStatement

import com.google.inject.{Inject, Singleton}
import com.twitter.util.Time
import givers.backdoor.framework.models._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.libs.json.Json
import slick.jdbc.{JdbcProfile, PositionedParameters, SQLActionBuilder}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class HistoryEntryService @Inject()(
  val dbConfigProvider: DatabaseConfigProvider,
  userService: UserService
)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import dbConfig.profile.api._

  val query = TableQuery[HistoryEntryTable]

  private[this] def getRawSql(sql: SQLActionBuilder): String = {
    val session = db.createSession()
    try {
      val statement = session.conn.prepareStatement(sql.queryParts.map(String.valueOf).mkString)
      sql.unitPConv.apply((), new PositionedParameters(statement))
      statement.unwrap(classOf[PreparedStatement]).toString
    } finally {
      session.close()
    }
  }

  def getRiches(exclusiveMaxIdOpt: Option[Long], limit: Int): Future[Seq[RichHistoryEntry]] = {
    get(exclusiveMaxIdOpt, limit).flatMap(hydrate)
  }

  def get(exclusiveMaxIdOpt: Option[Long], limit: Int): Future[Seq[HistoryEntry]] = {
    db
      .run {
        query
          .filter { q =>
            exclusiveMaxIdOpt.map { exclusiveMaxId => q.id < exclusiveMaxId }.getOrElse(LiteralColumn(true))
          }
          .sortBy(_.id.desc)
          .take(limit)
          .result
      }
  }

  private[this] def hydrate(items: Seq[HistoryEntry]): Future[Seq[RichHistoryEntry]] = {
    val fUsers = userService.getByIds(items.map(_.userId).toSet)

    for {
      users <- fUsers.map { us => us.map { u => u.id -> u }.toMap }
    } yield {
      items.map { item =>
        RichHistoryEntry(
          base = item,
          user = users(item.userId)
        )
      }
    }
  }

  def create(table: String, original: Row, sql: SQLActionBuilder, committer: User): Future[HistoryEntry] = {
    val entry = HistoryEntry(
      id = -1L,
      userId = committer.id,
      table = table,
      originalRecord = Json.obj(original.fields.map { f => f.column.name -> Json.toJsFieldJsValueWrapper(f.valueJson) }:_*),
      sql = getRawSql(sql),
      performedAt = Time.now
    )

    db
      .run {
        (query returning query.map(_.id)) += entry
      }
      .map { insertedId =>
        entry.copy(id = insertedId)
      }
  }
}
