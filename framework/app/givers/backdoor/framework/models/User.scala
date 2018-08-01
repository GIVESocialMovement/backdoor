package givers.backdoor.framework.models

import givers.backdoor.Permission
import play.api.libs.json.Json
import slick.jdbc.PostgresProfile.api._


object User {
  val COOKIE_KEY_ID = "userId"
  val COOKIE_KEY_SECRET = "secret"
}

case class User(
  id: Long,
  email: String,
  cookieSecret: String
) {
  def toJson = {
    Json.obj(
      "id" -> id,
      "email" -> email
    )
  }
}

case class RichUser(
  base: User,
  permission: Permission
)

class UserTable(tag: Tag) extends Table[User](tag, "users") {

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def cookieSecret = column[String]("cookie_secret")

  def * = (id, email, cookieSecret) <> ((User.apply _).tupled, User.unapply)
}
