package givers.backdoor.testproject

import givers.backdoor.Webhook
import givers.backdoor.Webhook.{Method, Target}
import givers.backdoor.framework.models.{Field, LongValue}
import play.api.Configuration


class Webhooks(config: Configuration) extends givers.backdoor.Webhooks {
  val webhookTargetUrl = config.get[String]("target.webhookUrl")

  val TABLES = Map(
    "examples" -> Seq(
      Webhook(
        methods = Set(Method.Insert, Method.Update),
        computeTarget = { (entity, _) =>
          Target(
            httpMethod = "POST",
            url = s"$webhookTargetUrl?id=${entity("id").asInstanceOf[Field[LongValue]].value.value}"
          )
        }
      )
    )
  )

  def get(table: String) = TABLES.getOrElse(table, Seq.empty)
}