package givers.backdoor.framework.services

import java.security.MessageDigest

import com.google.inject.{Inject, Singleton}
import com.twitter.conversions.time._
import com.twitter.util.Time
import givers.backdoor.framework.libraries.PlayConfig
import givers.backdoor.framework.models.{LoginCallback, LoginCallbackTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random


object LoginCallbackService {

  def isChecksumValid(checksum: String, id: Long, secret: String, playSecret: String): Boolean = {
    checksum == generateChecksum(id, secret, playSecret)
  }

  def generateChecksum(id: Long, secret: String, playSecret: String): String = {
    val checksum = MessageDigest.getInstance("SHA-256").digest(("LoginCallback" + id + "-" + secret + "-" + playSecret).getBytes("UTF-8"))
    checksum.map("%02X" format _).mkString
  }
}


@Singleton
class LoginCallbackService @Inject()(
  val dbConfigProvider: DatabaseConfigProvider,
  config: PlayConfig
)(implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import LoginCallbackService._
  import dbConfig.profile.api._

  val query = TableQuery[LoginCallbackTable]

  def create(originUrl: String): Future[LoginCallback] = {
    val login = LoginCallback(
      id = -1L,
      secretKey = Random.alphanumeric.take(128).mkString,
      originUrl = originUrl,
      status = LoginCallback.Status.Pending,
      createdAt = Time.now)

    db.run {
      (query returning query.map(_.id)) += login
    }.map { insertedId =>
      login.copy(
        id = insertedId,
        secretKey = generateChecksum(insertedId, login.secretKey, config.SECRET)
      )
    }
  }

  def getByState(state: String): Future[Option[LoginCallback]] = {
    import LoginCallback._

    val Array(id, checksum) = state.split("-", 2)

    db
      .run {
        query.filter { q => q.id === id.toLong && q.status === LoginCallback.Status.Pending }.take(1).result
      }
      .map {
        _.headOption
          .filter { login => login.createdAt > (Time.now - 10.minutes)}
          .filter { login =>
            isChecksumValid(checksum, login.id, login.secretKey, config.SECRET)
          }
          .map { login =>
            login.copy(
              secretKey = generateChecksum(login.id, login.secretKey, config.SECRET)
            )
          }
      }
      .flatMap {
        case None => Future(None)
        case Some(login) => finish(login).map { _ => Some(login) }
      }
  }

  private[this] def finish(login: LoginCallback): Future[Unit] = {
    db
      .run {
        query.filter(_.id === login.id).map(_.status).update(LoginCallback.Status.Finished)
      }
      .map { _ => () }
  }
}
