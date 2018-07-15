package givers.backdoor.framework.libraries

import givers.backdoor.framework.models.{Filter, Sort}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{JsValue, Json}

object Base64 {
  def rawEncode(s: String): String = {
    java.util.Base64.getEncoder.encodeToString(s.getBytes("UTF-8"))
  }

  def encodeString(s: String): String = {
    rawEncode(toJson(s).toString)
  }

  def encodeSeqString(ss: Seq[String]): String = {
    rawEncode(toJson(ss).toString)
  }

  def encodeInt(i: Int): String = {
    rawEncode(toJson(i).toString)
  }

  def encodeLong(i: Long): String = {
    rawEncode(toJson(i).toString)
  }

  def encodeBoolean(b: Boolean): String = {
    rawEncode(toJson(b).toString)
  }

  def encodeOptString(opt: Option[String]): String = {
    rawEncode(toJson(opt).toString)
  }

  def encodeJsValue(jsValue: JsValue): String = {
    rawEncode(jsValue.toString)
  }

  def encodeOptJsValue(jsValueOpt: Option[JsValue]): String = {
    rawEncode(toJson(jsValueOpt).toString)
  }

  def encodeSeqJsValue(jsValues: Seq[JsValue]): String = {
    rawEncode(toJson(jsValues).toString)
  }

  def encodeMap(jsMap: Map[String, String]): String = {
    rawEncode(toJson(jsMap).toString)
  }

  def encodeFilters(filters: Seq[Filter]): String = {
    val result = filters.sortBy(_.column.name).flatMap { filter =>
      filter.values
        .map {
          case Filter.Null => "null"
          case Filter.NotNull => "notnull"
          case Filter.String(s) => s"${Filter.STRING_PREFIX}$s"
        }
        .map { value => Json.obj("column" -> filter.column.name, "value" -> value) }
    }

    rawEncode(toJson(result).toString)
  }

  def encodeSorts(sorts: Seq[Sort]): String = {
    encodeMap(sorts.map { s => s.column.name -> s.direction.toString }.toMap)
  }
}
