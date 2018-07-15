package givers.backdoor.framework.services

import java.security.MessageDigest

import com.google.inject.{Inject, Singleton}
import givers.backdoor.framework.libraries.PlayConfig
import givers.backdoor.framework.models.{User, UserTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc.Cookies
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

object UserService {
  def isChecksumValid(checksum: String, userId: Long, cookieSecret: String, playSecret: String): Boolean = {
    checksum == generateChecksum(userId, cookieSecret, playSecret)
  }

  def generateChecksum(userId: Long, cookieSecret: String, playSecret: String): String = {
    val checksum = MessageDigest.getInstance("SHA-256").digest(("User" + userId + "-" + cookieSecret + "-" + playSecret).getBytes("UTF-8"))
    checksum.map("%02X" format _).mkString
  }

  def sanitizeEmail(email: String) = {
    email.toLowerCase.trim
  }

  // This doesn't need to be unique. It needs to be secret and randomised.
  def generateCookieSecret(): String = {
    Random.alphanumeric.take(128).mkString
  }
}


@Singleton
class UserService @Inject()(
  val dbConfigProvider: DatabaseConfigProvider,
  config: PlayConfig
)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import UserService._
  import dbConfig.profile.api._

  val query = TableQuery[UserTable]

  def create(email: String): Future[User] = {
    val user = User(
      id = -1L,
      email = sanitizeEmail(email),
      cookieSecret = generateCookieSecret()
    )

    db
      .run {
        (query returning query.map(_.id)) += user
      }
      .map { insertedId => user.copy(id = insertedId) }
  }

  def getByEmail(email: String): Future[Option[User]] = {
    db
      .run {
        query.filter(_.email === sanitizeEmail(email)).take(1).result
      }
      .map(_.headOption)
  }

  def getByIds(ids: Set[Long]): Future[Seq[User]] = {
    db
      .run {
        query.filter(_.id inSet ids).result
      }
  }

  def getById(id: Long): Future[Option[User]] = {
    getByIds(Set(id)).map(_.headOption)
  }

  def getUserFromCookies(cookies: Cookies): Future[Option[User]] = {
    import givers.backdoor.framework.libraries.PlayCookie._

    val resultOpt = for {
      userId <- cookies.get(User.COOKIE_KEY_ID).map(_.decodedValue.toLong)
      checksum <- cookies.get(User.COOKIE_KEY_SECRET).map(_.decodedValue)
    } yield {
      getById(userId.toLong).map { userOpt =>
        userOpt.filter { user =>
          UserService.isChecksumValid(checksum, user.id, user.cookieSecret, config.SECRET)
        }
      }
    }

    resultOpt.getOrElse(Future(None))
  }
}
