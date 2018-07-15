package givers.backdoor.framework.models

import java.sql.Timestamp

import givers.backdoor.Permission
import givers.backdoor.framework.libraries.PlayConfig
import org.postgresql.util.HStoreConverter
import play.api.libs.json._
import slick.jdbc.PositionedResult

import scala.collection.JavaConverters._


case class AccessibleColumn(
  base: BaseColumn,
  scope: Permission.Scope.Value
) {
  def toJson = {
    base.toJson ++ Json.obj(
      "canRead" -> scope.canRead,
      "canEdit" -> (scope.canWrite && base.editable)
    )
  }
}

object ComputedColumn {

  def convert(fields: Seq[BaseField[_]]): Map[String, BaseField[_]] = {
    fields.map { field =>
      field.column.name -> field
    }.toMap
  }

  def process(columns: Seq[AccessibleColumn], row: Row): Row = {
    val fieldByName = ComputedColumn.convert(row.fields)

    row.copy(
      fields = columns.map { column =>
        column.base match {
          case c: Column => fieldByName(c.name)
          case computed: ComputedColumn => Field(computed.compute(fieldByName), computed)
        }
      }
    )
  }
}

sealed abstract class BaseColumn {
  def name: String
  def editable: Boolean
  def sortable: Boolean
  def filterable: Boolean
  def isPrimaryKey: Boolean
  def toJson: JsObject
  def getCanonicalType: String
}

case class ComputedColumn(
  name: String,
  compute: Map[String, BaseField[_]] => Value
) extends BaseColumn {
  val editable = false
  val sortable = false
  val filterable = false
  val isPrimaryKey = false
  def getCanonicalType = throw new IllegalArgumentException(s"ComputedColumn ($name) doesn't have a type.")
  def toJson = {
    Json.obj(
      "name" -> name,
      "editable" -> editable,
      "sortable" -> sortable,
      "filterable" -> filterable,
    )
  }
}

case class Column(
  name: String,
  dataType: String,
  udtName: String,
  isNullable: Boolean,
  isPrimaryKey: Boolean,
  rawDefaultOpt: Option[String],
  charMaxLengthOpt: Option[Int],
) extends BaseColumn {
  val isAutoIncremental = rawDefaultOpt.exists(_.toLowerCase.startsWith("nextval("))
  val isArray = dataType == "ARRAY"
  val editable = !isAutoIncremental
  val sortable = true
  val filterable = true
  def arraySubtype = if (isArray) { udtName.substring(1) } else { throw new IllegalArgumentException(s"The column '$name' is not an array.") }

  def getCanonicalType = {
    if (isArray) {
      s"$arraySubtype[]"
    } else if (isAutoIncremental) {
      dataType match {
        case "bigint" => "bigserial"
        case "int" => "serial"
        case "smallint" => "smallserial"
      }
    } else {
      udtName + charMaxLengthOpt.map { length => s"($length)" }.getOrElse("")
    }
  }

  def defaultOpt = {
    if (isAutoIncremental) {
      None
    } else {
      rawDefaultOpt
    }
  }

  def getSpecForCreateTable = {
    val notNull = if (isNullable) { "NULL" } else { "NOT NULL" }
    val default = defaultOpt.map { d => s"'$d'" }.getOrElse("")
    val primaryKey = if (isPrimaryKey) { "PRIMARY KEY" } else { "" }
    s""""$name" $getCanonicalType $primaryKey $notNull $default"""
  }

  def cast(s: String): Value = {
    udtName match {
      case "int8" => LongValue(s.toLong)
      case "int4" => IntValue(s.toInt)
      case "int2" => IntValue(s.toInt)
      case "bool" => BooleanValue(s.toBoolean)
      case "char" => StringValue(s)
      case "varchar" => StringValue(s)
      case "text" => StringValue(s)
      case "json" => JsonValue(Json.parse(s))
      case "jsonb" => JsonValue(Json.parse(s))
      case "hstore" => HStoreValue(Json.parse(s).as[Map[String, String]])
      case "timestamp" => TimestampValue(Timestamp.valueOf(s))
      case _ => StringValue(s)
    }
  }

  def getNextBaseField(item: PositionedResult): BaseField[_] = {
    if (isArray) {
      item.skip
      SeqField(
        values = item.rs.getArray(item.currentPos).getArray.asInstanceOf[Array[Any]].map { v =>
          arraySubtype match {
            case "int8" => LongValue(v.asInstanceOf[Long])
            case "int4" => IntValue(v.asInstanceOf[Int])
            case "int2" => IntValue(v.asInstanceOf[Int])
            case "bool" => BooleanValue(v.asInstanceOf[Boolean])
            case "char" => StringValue(v.asInstanceOf[String])
            case "varchar" => StringValue(v.asInstanceOf[String])
            case "text" => StringValue(v.asInstanceOf[String])
            case "timestamp" => TimestampValue(v.asInstanceOf[Timestamp])
            case _ => StringValue(v.asInstanceOf[String])
          }
        },
        column = this
      )
    } else if (isNullable) {
      OptionalField(
        valueOpt = udtName match {
          case "int8" => item.nextLongOption().map(LongValue.apply)
          case "int4" => item.nextIntOption().map(IntValue.apply)
          case "int2" => item.nextIntOption().map(IntValue.apply)
          case "bool" => item.nextBooleanOption().map(BooleanValue.apply)
          case "char" => item.nextStringOption().map(StringValue.apply)
          case "varchar" => item.nextStringOption().map(StringValue.apply)
          case "text" => item.nextStringOption().map(StringValue.apply)
          case "json" => item.nextStringOption().map { s => JsonValue(Json.parse(s)) }
          case "jsonb" => item.nextStringOption().map { s => JsonValue(Json.parse(s)) }
          case "hstore" => item.nextStringOption().map { s => HStoreValue(HStoreConverter.fromString(s).asScala.toMap) }
          case "timestamp" => item.nextTimestampOption().map(TimestampValue.apply)
          case _ => item.nextStringOption().map(StringValue.apply)
        },
        column = this
      )
    } else {
      val v: Value =  udtName match {
        case "int8" => LongValue(item.nextLong())
        case "int4" => IntValue(item.nextInt())
        case "int2" => IntValue(item.nextInt())
        case "bool" => BooleanValue(item.nextBoolean())
        case "char" => StringValue(item.nextString())
        case "varchar" => StringValue(item.nextString())
        case "text" => StringValue(item.nextString())
        case "json" => JsonValue(Json.parse(item.nextString()))
        case "jsonb" => JsonValue(Json.parse(item.nextString()))
        case "hstore" => HStoreValue(HStoreConverter.fromString(item.nextString()).asScala.toMap)
        case "timestamp" => TimestampValue(item.nextTimestamp())
        case _ => StringValue(item.nextString())
      }
      Field(
        value = v,
        column = this
      )
    }
  }

  def toJson = {
    Json.obj(
      "name" -> name,
      "dataType" -> udtName,
      "isNullable" -> isNullable,
      "isPrimaryKey" -> isPrimaryKey,
      "isAutoIncremental" -> isAutoIncremental,
      "editable" -> editable,
      "sortable" -> sortable,
      "filterable" -> filterable
    )
  }
}

