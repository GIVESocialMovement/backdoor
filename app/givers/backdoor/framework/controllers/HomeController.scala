package givers.backdoor.framework.controllers

import com.google.inject.{Inject, Singleton}
import givers.backdoor.framework.libraries.{ControllerContext, PlayConfig}
import givers.backdoor.framework.models.Filter
import givers.backdoor.framework.models.Sort.Direction
import givers.backdoor.framework.services.{AccessService, AccessibleTableService}

object HomeController {
  val FILTER_PREFIX = "f-"
  val FILTER_VALUE_STRING_PREFIX = "v-"
  val PAGE_SIZE = 20
}

@Singleton
class HomeController @Inject()(
  tableService: AccessibleTableService,
  accessService: AccessService
)(
  implicit cc: ControllerContext,
  config: PlayConfig
) extends BaseController() {

  import HomeController._

  def index = asyncAuthenticated { implicit context =>
    tableService.getTables().map { tables =>
      Ok(givers.backdoor.framework.views.html.index(tables))
    }
  }

  private[this] def filterValueFromString(s: String): Filter.Value = {
    if (s == "null") {
      Filter.Null
    } else if (s == "notnull") {
      Filter.NotNull
    } else if (s.startsWith(FILTER_VALUE_STRING_PREFIX)) {
      Filter.String(s.substring(FILTER_VALUE_STRING_PREFIX.length))
    } else {
      throw new Exception(s"Invalid value: $s")
    }
  }

  private[this] def getRawFilters(queries: Map[String, Seq[String]]): Map[String, Seq[Filter.Value]] = {
    queries
      .filter { case (key, _) => key.startsWith(FILTER_PREFIX) }
      .map { case (key, values) =>
        key.substring(FILTER_PREFIX.length) -> values.map(filterValueFromString)
      }
  }

  private[this] def getRawSorts(queries: Map[String, Seq[String]]): Map[String, Direction.Value] = {
    queries.getOrElse("sort", Seq.empty).mkString(",").split(",").map(_.trim).filter(_.nonEmpty)
      .map { value =>
        val tokens = value.split("\\.")

        if (tokens.length != 2) {
          throw new Exception(s"Invalid value of sort - $value")
        }

        val sanitizedDirectionInput = tokens(1).toLowerCase

        val direction = if (sanitizedDirectionInput == "desc") {
          Direction.Desc
        } else if (sanitizedDirectionInput == "asc") {
          Direction.Asc
        } else {
          throw new Exception(s"Invalid direction: ${tokens(1)}")
        }

        tokens.head -> direction
      }
      .toMap
  }

  private[this] def getRawVisibleColumns(queries: Map[String, Seq[String]]): Seq[String] = {
    queries.getOrElse("columns", Seq.empty).mkString(",").split(",").map(_.trim).filter(_.nonEmpty)
  }

  def showTable(
    table: String,
    page: Int
  ) = asyncAuthenticatedTable(table, parse.anyContent) { table => implicit context =>
    for {
      filters <- tableService.getFilters(table, getRawFilters(context.request.queryString))
      sorts <- tableService.getSorts(table, getRawSorts(context.request.queryString))
      allColumns <- tableService.getColumns(table)
      visibleColumns <- tableService.getVisibleColumns(table, getRawVisibleColumns(context.request.queryString))
      total <- tableService.getTotalCount(table, filters)
      rows <- tableService.getRows(table, visibleColumns, filters, sorts, (page - 1) * PAGE_SIZE, PAGE_SIZE)
    } yield {
      Ok(givers.backdoor.framework.views.html.table(
        table = table,
        allColumns = allColumns,
        visibleColumns = visibleColumns,
        rows = rows,
        filters = filters,
        sorts = sorts,
        page = page,
        pageSize = PAGE_SIZE,
        total = total
      ))
    }
  }
}
