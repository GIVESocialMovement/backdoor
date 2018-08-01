package givers.backdoor.framework.models

import com.twitter.util.Time
import slick.jdbc.PostgresProfile.api._

object LoginCallback {
  object Status extends Enumeration {
    val Pending, Finished = Value
  }

  def statusToSqlType(status: Status.Value) = status.toString
  implicit val StatusColumnType = MappedColumnType.base[Status.Value, String](statusToSqlType, Status.withName)
}

case class LoginCallback(
  id: Long,
  secretKey: String,
  originUrl: String,
  status: LoginCallback.Status.Value,
  createdAt: Time
) {
  val state = s"$id-$secretKey"
}

class LoginCallbackTable(tag: Tag) extends Table[LoginCallback](tag, "login_callbacks") {

  import givers.backdoor.framework.libraries.ExtraPostgresColumnType._
  import LoginCallback._

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def secretKey = column[String]("secret_key")
  def originUrl = column[String]("origin_url")
  def status = column[Status.Value]("status")
  def createdAt = column[Time]("created_at")

  def * = (id, secretKey, originUrl, status, createdAt) <> ((LoginCallback.apply _).tupled, LoginCallback.unapply)
}
