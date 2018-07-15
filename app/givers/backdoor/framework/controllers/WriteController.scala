package givers.backdoor.framework.controllers

import com.google.inject.{Inject, Singleton}
import givers.backdoor.framework.libraries.{ControllerContext, PlayConfig}
import givers.backdoor.framework.models.{BaseField, Column}
import givers.backdoor.framework.services.{AccessService, TableService}
import givers.backdoor.framework.services.TableService.{DeleteData, UpdateData}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json.{obj, toJson, _}
import play.api.libs.json.{JsObject, JsValue}

object WriteController {
  val DELETE_FORM = Form(
    mapping(
      "primaryKeyColumn" -> nonEmptyText,
      "primaryKeyValue" -> nonEmptyText,
    )(DeleteData.apply)(DeleteData.unapply)
  )

  val UPDATE_FORM = Form(
    mapping(
      "column" -> text,
      "newValue" -> text,
      "isNull" -> boolean,
      "primaryKeyColumn" -> nonEmptyText,
      "primaryKeyValue" -> nonEmptyText,
    )(UpdateData.apply)(UpdateData.unapply)
  )
}

@Singleton
class WriteController @Inject()(
  tableService: TableService,
  accessService: AccessService
)(
  implicit cc: ControllerContext,
  config: PlayConfig
) extends BaseController() {

  import WriteController._

  def newRow(table: String) = asyncAuthenticatedTable(table, parse.anyContent) { table => implicit context =>
    if (!table.canCreate) {
      throw new Exception("Forbidden")
    }

    tableService.getColumns(table.base.name)
      .map { columns =>
        Ok(givers.backdoor.framework.views.html.newRow(table.base.name, columns))
      }
  }

  private[this] def getCreateMap(json: JsValue, columns: Seq[Column]): Seq[BaseField[_]] = {
    val columnMap = columns.map { c => c.name -> c }.toMap
    json.as[JsObject].value.toSeq.flatMap { case (columnName, field) =>
      columnMap.get(columnName).map { column =>

        val fieldValue = field.as[JsObject].value.get("value").map(_.as[String]).getOrElse("")
        val isNull = field.as[JsObject].value.get("isNull").exists(_.as[Boolean])

        BaseField.get(column, fieldValue, isNull)
      }
    }
  }

  def create(table: String) = asyncAuthenticatedTable(table, parse.json) { table => implicit context =>
    if (!table.canCreate) {
      throw new Exception("Forbidden")
    }

    for {
      columns <- tableService.getColumns(table.base.name)
      _ <- tableService.insert(table.base.name, getCreateMap(context.request.body, columns), context.loggedInUser)
    } yield {
      Ok(toJson(obj("success" -> true)))
    }
  }

  def remove(table: String) = asyncAuthenticatedTable(table, parse.json) { table => implicit context =>
    Thread.sleep(10000)
    handleForm(DELETE_FORM, "delete") { data =>

      for {
        primaryKeyColumn <- tableService.getColumn(table.base.name, data.primaryKeyColumn).map {
          _.getOrElse { throw new Exception(s"The column '${data.primaryKeyColumn}' doesn't exist in the table '$table'.")}
        }
        _ <- tableService.delete(
          table = table.base.name,
          primaryKey = BaseField.get(primaryKeyColumn, data.primaryKeyValue, false),
          committer = context.loggedInUser
        )
      } yield {
        Ok(toJson(obj(
          "success" -> true
        )))
      }
    }
  }

  def update(table: String) = asyncAuthenticatedTable(table, parse.json) { table => implicit context =>
    handleForm(UPDATE_FORM, "update") { data =>

      for {
        column <- tableService.getColumn(table.base.name, data.column).map { colOpt =>
          val col = colOpt.getOrElse { throw new Exception(s"The column '${data.column}' doesn't exist in the table '$table'.")}

          if (!accessService.getScope(context.loggedInUser, table.base, col).canWrite) {
            throw new Exception("Forbidden")
          }

          col
        }
        primaryKeyColumn <- tableService.getColumn(table.base.name, data.primaryKeyColumn).map {
          _.getOrElse { throw new Exception(s"The column '${data.primaryKeyColumn}' doesn't exist in the table '$table'.")}
        }
        _ <- tableService.update(
          table = table.base.name,
          field = BaseField.get(column, data.newValue, data.isNull),
          primaryKey = BaseField.get(primaryKeyColumn, data.primaryKeyValue, false),
          committer = context.loggedInUser
        )
        field <- tableService.get(table.base.name, column, primaryKeyColumn, data.primaryKeyValue).map {
          _.getOrElse { throw new Exception("Impossible")}
        }
      } yield {
        Ok(toJson(obj(
          "success" -> true,
          "field" -> field.toJson(force = true)
        )))
      }
    }
  }
}
