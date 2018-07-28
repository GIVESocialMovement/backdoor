package givers.backdoor.testproject

import com.twitter.util.Time
import givers.backdoor.framework.models._
import play.api.Configuration

class ComputedColumns(config: Configuration) extends givers.backdoor.ComputedColumns {
  val baseUrl = config.get[String]("target.webhookUrl")

  val TABLES = Map(
    "examples" -> Seq(
      ComputedColumn(
        name = "age_in_days",
        compute = { entity =>
          val birthdate = Time.fromMilliseconds(entity("birthdate").asInstanceOf[Field[TimestampValue]].value.value.getTime)
          StringValue(s"${(Time.now - birthdate).inDays} days")
        }
      )
    )
  )

  def get(table: String): Seq[ComputedColumn] = TABLES.getOrElse(table, Seq.empty)
}