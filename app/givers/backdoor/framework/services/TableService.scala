package givers.backdoor.framework.services

import com.google.inject.{Inject, Singleton}
import givers.backdoor.Webhook
import givers.backdoor.framework.libraries.{Helper, PlayConfig}
import givers.backdoor.framework.models.Sort.Direction
import givers.backdoor.framework.models._
import slick.jdbc.GetResult
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.{ExecutionContext, Future}


object TableService {
  case class DeleteData(
    primaryKeyColumn: String,
    primaryKeyValue: String
  )

  case class UpdateData(
    column: String,
    newValue: String,
    isNull: Boolean,
    primaryKeyColumn: String,
    primaryKeyValue: String
  )

  val SECURE_STRING_REGEX = "^[a-zA-Z0-9_]+$"

  def checkSecurity(table: String): Unit = {
    assert(table.matches(SECURE_STRING_REGEX), s"The table '$table' doesn't exist.")
  }
}


@Singleton
class TableService @Inject()(
  historyEntryService: HistoryEntryService,
  webhookService: WebhookService,
  playConfig: PlayConfig
)(implicit ec: ExecutionContext) {

  import TableService._

  val db = Database.forDataSource(
    ds = new slick.jdbc.DatabaseUrlDataSource {
      url = playConfig.SUPERVISED_DATABASE_URL
    },
    maxConnections = None
  )

  def insert(
    table: String,
    fields: Seq[BaseField[_]],
    committer: User
  ): Future[Row] = {
    checkSecurity(table)

    for {
      columns <- getColumns(table)
      getResult = GetResult { item =>
        Row(columns.map(_.getNextBaseField(item)))
      }
      sql = {
        val columnString = columns
          .map { c => s""""${c.name}"""" }.mkString(", ")

        Helper.concat(
          sql"INSERT INTO #$table (#${fields.map { f => s""""${f.column.name}"""" }.mkString(", ")}) VALUES (",
          fields.flatMap { f => Seq(sql", ", f.toSql) }.tail ++ Seq(sql")", sql" RETURNING #$columnString"):_*
        )
      }
      inserted <- db
        .run { sql.as[Row](getResult) }
        .map { inserteds =>
          assert(inserteds.size == 1)
          inserteds.head
        }
      _ <- historyEntryService.create(table, Row(Seq.empty), sql, committer)
      _ <- webhookService.process(table, Webhook.Method.Insert, inserted)
    } yield {
      inserted
    }
  }

  def delete(table: String, primaryKey: BaseField[_], committer: User): Future[Unit] = {
    checkSecurity(table)

    for {
      columns <- getColumns(table)
      getResult = GetResult { item =>
        Row(columns.map(_.getNextBaseField(item)))
      }
      sql = {
        val columnString = columns
          .map { c => s""""${c.name}"""" }.mkString(", ")

        Helper.concat(
          sql"DELETE FROM #$table WHERE #${primaryKey.column.name} = ",
          primaryKey.toSql,
          sql" RETURNING #$columnString"
        )
      }
      row <- getRow(table, primaryKey)

      deleted <- db
        .run { sql.as[Row](getResult) }
        .map { deleteds =>
          assert(deleteds.size == 1)
          deleteds.head
        }
      _ <- historyEntryService.create(
        table = table,
        original = row,
        sql = sql,
        committer = committer)
      _ <- webhookService.process(table, Webhook.Method.Delete, deleted)
    } yield {
      ()
    }
  }

  def update(
    table: String,
    field: BaseField[_],
    primaryKey: BaseField[_],
    committer: User
  ): Future[Unit] = {
    checkSecurity(table)

    for {
      columns <- getColumns(table)
      getResult = GetResult { item =>
        Row(columns.map(_.getNextBaseField(item)))
      }
      sql = {
        val columnString = columns
          .map { c => s""""${c.name}"""" }.mkString(", ")

        Helper.concat(
          sql"UPDATE #$table SET #${field.column.name} = ",
          field.toSql,
          sql" WHERE #${primaryKey.column.name} = ",
          primaryKey.toSql,
          sql" RETURNING #$columnString"
        )
      }
      row <- getRow(table, primaryKey)
      updated <- db
        .run { sql.as[Row](getResult) }
        .map { updateds =>
          assert(updateds.size == 1)
          updateds.head
        }
      _ <- historyEntryService.create(
        table = table,
        original = row,
        sql = sql,
        committer = committer)
      _ <- webhookService.process(table, Webhook.Method.Update, updated)
    } yield {
      ()
    }
  }

  def getRow(table: String, primaryKey: BaseField[_]): Future[Row] = {
    for {
      columns <- getColumns(table)
      rows <- getRows(
        table = table,
        columns = columns,
        filters = Seq(primaryKey.filter),
        sorts = Seq.empty,
        offset = 0,
        limit = 1
      )
    } yield {
      rows.headOption.getOrElse {
        throw new Exception(s"The row (${primaryKey.column.name}=${primaryKey.filter.values}) doesn't exist.")
      }
    }
  }

  def getColumn(table: String, column: String): Future[Option[Column]] = {
    for {
      columns <- getColumns(table)
    } yield {
      columns.find(_.name == column)
    }
  }

  def get(table: String, column: Column, primaryKeyColumn: Column, primaryKeyValue: String): Future[Option[BaseField[_]]] = {
    checkSecurity(table)

    for {
      rows <- getRows(
        table = table,
        columns = Seq(column),
        filters = Seq(Filter(primaryKeyColumn, Set(Filter.String(primaryKeyValue)))),
        sorts = Seq.empty,
        offset = 0,
        limit = 1
      )
    } yield {
      rows.headOption.map { row =>
        row.fields.find(_.column.name == column.name).get
      }
    }
  }
  private[this] def convertTable = GetResult { r => TableModel(r.<<) }

  def getTable(name: String): Future[Option[TableModel]] = {
    db
      .run {
        sql"""SELECT
             |  table_name
             |FROM information_schema.tables
             |WHERE table_schema='public' AND table_name = $name
             |ORDER BY table_name ASC;
             |""".stripMargin.as[TableModel](convertTable)
      }
      .map(_.headOption)
  }

  def getTables(): Future[Seq[TableModel]] = {
    db
      .run {
        sql"""SELECT
             |  table_name
             |FROM information_schema.tables
             |WHERE table_schema='public'
             |ORDER BY table_name ASC;
             |""".stripMargin.as[TableModel](convertTable)
      }
  }

  def getColumns(table: String): Future[Seq[Column]] = {
    checkSecurity(table)

    def getResult(primaryKeyNameOpt: Option[String]) = GetResult { r =>
      val name = r.<<[String]
      Column(
        name = name,
        dataType = r.<<,
        udtName = r.<<,
        isNullable = r.<<[String] == "YES",
        isPrimaryKey = primaryKeyNameOpt.contains(name),
        rawDefaultOpt = Option(r.<<[String]),
        charMaxLengthOpt = r.nextIntOption()
      )
    }
    for {
      primaryKeyNameOpt <- db
        .run {
          sql"""SELECT
               |  a.attname AS data_type
               |FROM pg_index i
               |JOIN pg_attribute a
               |ON a.attrelid = i.indrelid
               |   AND a.attnum = ANY(i.indkey)
               |WHERE i.indrelid = $table::regclass
               |AND i.indisprimary;
               |""".stripMargin.as[String]
        }
        .map(_.headOption)
      columns <- db
        .run {
          sql"""SELECT
               |  column_name, data_type, udt_name, is_nullable, column_default, character_maximum_length
               |FROM information_schema.columns
               |WHERE
               |  table_schema = 'public'
               |  AND table_name = $table
               |ORDER BY ordinal_position ASC;
               |""".stripMargin.as[Column](getResult(primaryKeyNameOpt))
        }
    } yield {
      columns
    }
  }

  def getTotalCount(
    table: String,
    filters: Seq[Filter]
  ): Future[Int] = {
    checkSecurity(table)

    db
      .run {
        Helper.concat(
          sql"SELECT COUNT(1) FROM #$table",
          buildWhere(filters)
        ).as[Int]
      }
      .map(_.head)
  }

  def getRows(
    table: String,
    columns: Seq[Column],
    filters: Seq[Filter],
    sorts: Seq[Sort],
    offset: Int,
    limit: Int
  ): Future[Seq[Row]] = {
    checkSecurity(table)

    val getResult = GetResult { item =>
      Row(columns.map(_.getNextBaseField(item)))
    }

    val sanitizedSorts = if (sorts.isEmpty) {
      columns
        .filter(_.isPrimaryKey)
        .map { col => Sort(column = col, direction = Direction.Desc) }
    } else {
      sorts
    }

    val orderByClause = if (sanitizedSorts.nonEmpty) {
      val sortString = sanitizedSorts
        .map { sort =>
          s""""${sort.column.name}" ${sort.direction.toString}"""
        }
        .mkString(", ")
      sql""" ORDER BY #$sortString"""
    } else {
      sql""
    }


    val columnString = columns
      .map { c => s""""${c.name}"""" }.mkString(", ")

    db
      .run {
        Helper.concat(
          sql"""SELECT #$columnString FROM "#$table"""",
          buildWhere(filters),
          orderByClause,
          sql" OFFSET $offset LIMIT $limit"
        ).as[Row](getResult)
      }
  }

  private[this] def buildWhere(filters: Seq[Filter]) = {
    val conditions = filters.map { filter =>
      Helper.or(
        filter.values.map {
          case Filter.Null => sql"#${filter.column.name} IS NULL"
          case Filter.NotNull => sql"#${filter.column.name} IS NOT NULL"
          case Filter.String(s) => Helper.concat(sql"#${filter.column.name} = ", BaseField.get(filter.column, s, isNull = false).toSql)
        }.toSeq:_*
      )
    }

    if (conditions.nonEmpty) {
      Helper.concat(sql" WHERE ", Helper.and(conditions:_*))
    } else {
      sql""
    }
  }
}
