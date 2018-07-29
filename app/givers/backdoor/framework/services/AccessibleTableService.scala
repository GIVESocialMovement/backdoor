package givers.backdoor.framework.services

import com.google.inject.{Inject, Singleton}
import givers.backdoor.ComputedColumns
import givers.backdoor.framework.libraries.{AuthenticatedContext, PlayConfig}
import givers.backdoor.framework.models.Sort.Direction
import givers.backdoor.framework.models._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AccessibleTableService @Inject()(
  tableService: TableService,
  access: AccessService,
  computedColumns: ComputedColumns
)(implicit ec: ExecutionContext) {

  def getTable(name: String)(implicit context: AuthenticatedContext[_]): Future[Option[AccessibleTable]] = {
    tableService.getTable(name)
      .map { tableOpt =>
        tableOpt.map { table =>
          AccessibleTable(
            base = table,
            canRead = access.canRead(context.loggedInUser.base, table),
            canCreate = access.canCreate(context.loggedInUser.base, table),
            canDelete = access.canDelete(context.loggedInUser.base, table)
          )
        }
      }
      .map(_.filter(_.canRead))
  }

  def getTables()(implicit context: AuthenticatedContext[_]): Future[Seq[AccessibleTable]] = {
    tableService.getTables()
      .map { tables =>
        tables.map { table =>
          AccessibleTable(
            base = table,
            canRead = access.canRead(context.user.base, table),
            canCreate = access.canCreate(context.user.base, table),
            canDelete = access.canDelete(context.user.base, table)
          )
        }
      }
      .map(_.filter(_.canRead))
  }

  def getColumns(table: AccessibleTable)(implicit context: AuthenticatedContext[_]): Future[Seq[AccessibleColumn]] = {
    assert(table.canRead)

    tableService.getColumns(table.base.name)
      .map { computedColumns.get(table.base.name) ++ _ }
      .map { columns =>
        columns.map { column =>
          AccessibleColumn(
            base = column,
            scope = access.getScope(context.loggedInUser.base, table.base, column)
          )
        }
      }
      .map(_.filter(_.scope.isAccessible))
  }

  def getTotalCount(
    table: AccessibleTable,
    filters: Seq[Filter]
  ): Future[Int] = {
    tableService.getTotalCount(table.base.name, filters)
  }

  def getRows(
    table: AccessibleTable,
    columns: Seq[AccessibleColumn],
    filters: Seq[Filter],
    sorts: Seq[Sort],
    offset: Int,
    limit: Int
  ): Future[Seq[Row]] = {
    tableService.getRows(
        table = table.base.name,
        columns = columns.flatMap { col =>
          col.base match {
            case _: ComputedColumn => None
            case c: Column => Some(c)
          }
        },
        filters = filters,
        sorts = sorts,
        offset = offset,
        limit = limit
      )
      .map { rows =>
        rows.map { row =>
          ComputedColumn.process(columns, row)
        }
      }
  }

  def getVisibleColumns(table: AccessibleTable, rawVisibleColumns: Seq[String])(implicit context: AuthenticatedContext[_]): Future[Seq[AccessibleColumn]] = {
    val columnNameSet = rawVisibleColumns.toSet
    getColumns(table)
      .map { columns =>
        val inaccessibleColumns = columnNameSet -- columns.map(_.base.name)
        if (inaccessibleColumns.nonEmpty) {
          throw new Exception(s"You cannot access the columns: ${inaccessibleColumns.mkString(", ")}")
        }

        columns.filter { column => columnNameSet.isEmpty || columnNameSet.contains(column.base.name) }
      }
  }

  def getFilters(table: AccessibleTable, rawFilters: Map[String, Seq[Filter.Value]])(implicit context: AuthenticatedContext[_]): Future[Seq[Filter]] = {
    val columnNameSet = rawFilters.keys.toSet

    getColumns(table)
      .map { columns =>
        val inaccessibleColumns = columnNameSet -- columns.map(_.base.name)
        if (inaccessibleColumns.nonEmpty) {
          throw new Exception(s"You cannot access the columns: ${inaccessibleColumns.mkString(", ")}")
        }

        columns.flatMap { column =>
          rawFilters.get(column.base.name).map { values =>
            Filter(
              column = column.base.asInstanceOf[Column],
              values = values.toSet
            )
          }
        }
      }
  }

  def getSorts(table: AccessibleTable, rawSorts: Map[String, Direction.Value])(implicit context: AuthenticatedContext[_]): Future[Seq[Sort]] = {
    val columnNameSet = rawSorts.keys.toSet

    getColumns(table)
      .map { columns =>
        val inaccessibleColumns = columnNameSet -- columns.map(_.base.name)
        if (inaccessibleColumns.nonEmpty) {
          throw new Exception(s"You cannot access the columns: ${inaccessibleColumns.mkString(", ")}")
        }

        columns.flatMap { column =>
          rawSorts.get(column.base.name).map { direction =>
            Sort(
              column = column.base.asInstanceOf[Column],
              direction = direction
            )
          }
        }
      }
  }
}
