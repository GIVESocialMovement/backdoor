package givers.backdoor.framework.models

import java.sql.Timestamp

import givers.backdoor.framework.libraries.Helper
import org.postgresql.util.HStoreConverter
import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._
import slick.jdbc.SQLActionBuilder

object BaseField {
  def get(column: Column, value: String, isNull: Boolean) = try {
    if (column.isArray) {
      assert(!isNull, s"The column '${column.name}' cannot have the null value.")
      val values = Json.parse(value).as[JsArray].value.map(_.as[String]).toList
      SeqField(
        values = values.map(column.cast),
        column = column
      )
    } else if (column.isNullable) {
      OptionalField(
        valueOpt = if (isNull) {
          None
        } else {
          Some(column.cast(value))
        },
        column = column
      )
    } else {
      assert(!isNull, s"The column '${column.name}' cannot have the null value.")
      Field(column.cast(value), column = column)
    }
  } catch { case e: Exception =>
    e.printStackTrace()
    throw new Exception(s"Unable to parse '$value' for the column '${column.name}' (type=${column.dataType})", e)
  }

  def preview(s: String): String = {
    if (s.length > 100) {
      s"${s.take(100)}..."
    } else {
      s
    }
  }
}

sealed abstract class Value {
  def toJson: JsValue
  def render: String
  def editableValue: String
  def toSql: SQLActionBuilder
}

sealed abstract class BaseField[T] {
  def column: BaseColumn
  def toJson(force: Boolean): JsValue
  def toSql: SQLActionBuilder
  def value: T
  def filter: Filter
  def valueJson: JsValue
}

case class Field[T <: Value](value: T, column: BaseColumn) extends BaseField[T] {
  def toJson(force: Boolean) = {
    Json.obj(
      "column" -> column.toJson,
      "value" -> value.toJson,
      "editableValue" -> value.editableValue,
      "renderedValue" -> value.render
    )
  }

  def toSql = value.toSql

  def filter = Filter(column.asInstanceOf[Column], Set(Filter.String(value.editableValue)))

  def valueJson = value.toJson
}

case class OptionalField[T <: Value](valueOpt: Option[T], column: BaseColumn) extends BaseField[Option[T]] {
  def toJson(force: Boolean) = {
    Json.obj(
      "column" -> column.toJson,
      "value" -> valueOpt.map(_.toJson),
      "editableValue" -> valueOpt.map(_.editableValue),
      "renderedValue" -> valueOpt.map(_.render)
    )
  }

  def toSql = valueOpt.map { value => value.toSql }.getOrElse(sql"NULL")

  val value = valueOpt

  def filter = Filter(column.asInstanceOf[Column], Set(valueOpt.map { v => Filter.String(v.editableValue) }.getOrElse(Filter.Null)))

  def valueJson = valueOpt.map(_.toJson).getOrElse(JsNull)
}

case class SeqField[T <: Value](values: Seq[T], column: BaseColumn) extends BaseField[Seq[T]] {
  def toJson(force: Boolean) = {
    Json.obj(
      "column" -> column.toJson,
      "value" -> values.map(_.toJson),
      "editableValue" -> Json.prettyPrint(JsArray(values.map { v => JsString(v.editableValue) })),
      "renderedValue" -> Json.prettyPrint(JsArray(values.map { v => JsString(v.render) }))
    )
  }

  def toSql = {
    val sqls = if (values.isEmpty) {
      Seq.empty
    } else {
      values.flatMap { v => Seq(sql", ", v.toSql) }.tail
    }

    val args = Seq(sql"ARRAY[") ++ sqls ++ Seq(sql"]", sql"::#${column.getCanonicalType}")
    Helper.concat(args.head, args.tail:_*)
  }

  val value = values

  def filter = {
    throw new Exception("SeqField doesn't support filter.")
  }

  def valueJson = JsArray(values.map(_.toJson))
}

case class HStoreValue(value: Map[String, String]) extends Value {
  import scala.collection.JavaConverters._

  def toJson = Json.toJson(Json.obj(value.mapValues { v => Json.toJsFieldJsValueWrapper(v) }.toSeq:_*))
  def editableValue = Json.prettyPrint(toJson)
  def render = xml.Utility.escape(Json.prettyPrint(toJson))
  def toSql = sql"${HStoreConverter.toString(value.asJava)}::hstore"
}

case class JsonValue(value: JsValue) extends Value {
  def toJson = value
  def editableValue = Json.prettyPrint(toJson)
  def render = xml.Utility.escape(Json.prettyPrint(toJson))
  def toSql = sql"${value.toString}::jsonb"
}

case class IntValue(value: Int) extends Value {
  def toJson = Json.toJson(value)
  def editableValue = value.toString
  def render = value.toString
  def toSql = sql"$value"
}

case class LongValue(value: Long) extends Value {
  def toJson = Json.toJson(value)
  def editableValue = value.toString
  def render = value.toString
  def toSql = sql"$value"
}

case class StringValue(value: String) extends Value {
  def toJson = Json.toJson(value)
  def editableValue = value.toString
  def render = xml.Utility.escape(BaseField.preview(value.toString))
  def toSql = sql"$value"
}

case class BooleanValue(value: Boolean) extends Value {
  def toJson = Json.toJson(value)
  def editableValue = value.toString
  def render = value.toString
  def toSql = sql"$value"
}

case class TimestampValue(value: Timestamp) extends Value {
  def toJson = Json.toJson(value.toString)
  def editableValue = value.toString
  def render = value.toString
  def toSql = sql"$value"
}

case class UrlValue(url: String) extends Value {
  def toJson = Json.toJson(url)
  def editableValue = url.toString
  def render = s"""<a href="$url">${xml.Utility.escape(BaseField.preview(url))}</a>"""
  def toSql = throw new NotImplementedError()
}

case class Row(
  fields: Seq[BaseField[_]]
) {
  def toJson(force: Boolean) = {
    val primaryKey = fields.find(_.column.isPrimaryKey).getOrElse { throw new Exception("Can't find the primary key column.") }
    Json.obj(
      "primaryKey" -> primaryKey.toJson(force = true),
      "fields" -> fields.map(_.toJson(force))
    )
  }
}
