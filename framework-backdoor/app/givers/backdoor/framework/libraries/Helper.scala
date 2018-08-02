package givers.backdoor.framework.libraries

import com.twitter.util.Time
import slick.jdbc.{PositionedParameters, SQLActionBuilder, SetParameter}
import views.html.helper

object Helper {

  val DUPLICATE_KEY_SQL_STATE = "23505"

  val BLACKLIST_URLS = Seq(
    "/facebook",
    "/user"
  )

  def formatTime(time: Time): String = {
    time.format("dd MMMM yyyy HH:mm:ss z Z")
  }

  def getEncodedRequestUri()(implicit context: BaseContext[_]): String = {
    val url = if (BLACKLIST_URLS.exists(context.request.uri.startsWith)) {
      "/"
    } else {
      context.request.uri
    }

    helper.urlEncode(url)
  }

  def urlDecode(string: String)(implicit codec: play.api.mvc.Codec): String = {
    java.net.URLDecoder.decode(string, codec.charset)
  }

  private[this] def merge(condition: SQLActionBuilder, sqls: Seq[SQLActionBuilder]): SQLActionBuilder = {
    import slick.jdbc.PostgresProfile.api._
    sqls.toList match {
      case Nil => sql""
      case head :: Nil => head
      case head :: tail =>
        Helper.concat(sql"(", Seq(head) ++ tail.map { s => Helper.concat(condition, s) } ++ Seq(sql")"): _*)
    }
  }

  def or(sqls: SQLActionBuilder*): SQLActionBuilder = {
    import slick.jdbc.PostgresProfile.api._
    merge(sql" OR ", sqls)
  }

  def and(sqls: SQLActionBuilder*): SQLActionBuilder = {
    import slick.jdbc.PostgresProfile.api._
    merge(sql" AND ", sqls)
  }

  def concat(first: SQLActionBuilder, sqls: SQLActionBuilder*): SQLActionBuilder = {
    sqls.headOption
      .map{ second =>
        val nextFirst = SQLActionBuilder(first.queryParts ++ second.queryParts, new SetParameter[Unit] {
          def apply(p: Unit, pp: PositionedParameters): Unit = {
            first.unitPConv.apply(p, pp)
            second.unitPConv.apply(p, pp)
          }
        })
        concat(nextFirst, sqls.tail:_*)
      }
      .getOrElse(first)
  }

}
