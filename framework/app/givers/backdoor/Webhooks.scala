package givers.backdoor

import givers.backdoor.Webhook.Target
import givers.backdoor.framework.libraries.PlayConfig
import givers.backdoor.framework.models.BaseField

case class Webhook(
  methods: Set[Webhook.Method.Value],
  computeTarget: (Map[String, BaseField[_]], PlayConfig) => Target
)

abstract class Webhooks {
  def get(table: String): Seq[Webhook]
}

object Webhook {

  case class Target(
    httpMethod: String,
    url: String
  )

  object Method {
    sealed abstract class Value
    object Insert extends Value
    object Update extends Value
    object Delete extends Value
  }
}
