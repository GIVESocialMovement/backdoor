package givers.backdoor.framework.services

import com.google.inject.{Inject, Singleton}
import givers.backdoor.framework.libraries.PlayConfig
import givers.backdoor.framework.models.{ComputedColumn, Row}
import givers.backdoor.{Webhook, Webhooks}
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}


@Singleton
class WebhookService @Inject()(
  playConfig: PlayConfig,
  webhooks: Webhooks,
  ws: WSClient
)(implicit ec: ExecutionContext) {
  def process(table: String, method: Webhook.Method.Value, row: Row): Future[Unit] = {
    val fieldByName = ComputedColumn.convert(row.fields)

    Future
      .sequence(
        webhooks.get(table)
          .filter(_.methods.contains(method))
          .map { webhook =>
             val target = webhook.computeTarget(fieldByName, playConfig)
             ws
               .url(target.url)
               .execute(target.httpMethod)
               .map { resp =>
                 if (resp.status != 200) {
                   throw new Exception(s"The webhook to ${target.url} (status=${resp.status}) failed. Please try updating the row again to trigger the webhook.")
                 } else {
                   ()
                 }
               }
          }
      )
      .map { _ => () }
  }
}
