package helpers

import java.util.concurrent.{TimeUnit, TimeoutException}

import akka.actor.ActorSystem
import akka.stream.Materializer
import com.twitter.util.{Time, TimeControl}
import com.typesafe.config.Config
import givers.backdoor.{ComputedColumns, Permissions, Webhooks}
import givers.backdoor.framework.libraries.PlayConfig
import givers.backdoor.framework.models.Column
import org.mockito.ArgumentMatcher
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer
import org.scalatest._
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.mockito.MockitoSugar
import play.api.data.Form
import play.api.db.evolutions.EvolutionsApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsResultException, JsValue, Json}
import play.api.mvc.ControllerComponents
import play.api.{Configuration, Environment, Mode}
import slick.jdbc.{JdbcBackend, JdbcProfile}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.language.implicitConversions
import scala.reflect.ClassTag

object BaseSpec {
  class Module extends play.api.inject.Module {
    def bindings(environment: Environment, configuration: Configuration) = Seq(
      bind[givers.backdoor.Webhooks].toInstance(new Webhooks {
        def get(table: String) = Seq.empty
      }),
      bind[givers.backdoor.ComputedColumns].toInstance(new ComputedColumns {
        def get(table: String) = Seq.empty
      }),
      bind[givers.backdoor.Permissions].toInstance(new Permissions {
        def get(email: String) = None
      })
    )
  }

  val PORT = 9001
  def generateAppConfig = Map(
    "slick.dbs.default.db.properties.url" -> "postgres://backdoor_test_user:test@localhost:5432/backdoor_test",

    "target.databaseUrl" -> "postgres://backdoor_test_user:test@localhost:5432/backdoor_target_test",

    "play.evolutions.enabled" -> false,
    "play.filters.cors.allowedOrigins" -> Seq(s"http://localhost:$PORT"),
    "play.filters.hosts.allowed" -> Seq(s"localhost:$PORT"),
    "http.origin" -> s"http://localhost:$PORT",

    "auth0.clientId" -> "fake_client_id",
    "auth0.clientSecret" -> "fake_client_secret",
    "auth0.domain" -> "fake_domain"
  )
  lazy val app = {
    sys.props.put("config.resource", "backdoor.conf")
    new GuiceApplicationBuilder()
      .configure(Configuration.from(generateAppConfig))
      .bindings(new Module)
      .in(Mode.Test)
      .build()
  }

  def ensureDatabase(database: JdbcBackend#Database, databaseName: String): Unit = {
    import givers.backdoor.framework.libraries.PostgresProfile.api._
    import scala.concurrent.ExecutionContext.Implicits.global

    Await.result(
      awaitable = database
        .run {
          sql"SELECT 1 FROM pg_database WHERE datname = $databaseName".as[Int]
        }
        .flatMap { items =>
          if (items.isEmpty) {
            database.run { sqlu"CREATE DATABASE #$databaseName;" }
          } else {
            Future(())
          }
        },
      atMost = Duration.apply(30, TimeUnit.SECONDS)
    )
  }


  lazy val dbConfigProvider = app.injector.instanceOf[DatabaseConfigProvider]
  lazy val dbConfig = dbConfigProvider.get[JdbcProfile]
  lazy val db = {
    import dbConfig.profile.api._
    import scala.concurrent.ExecutionContext.Implicits.global

    ensureDatabase(dbConfig.db, "backdoor_target_test")
    ensureDatabase(dbConfig.db, "backdoor_test_dummy")

    val dummyDb = dbConfig.profile.api.Database.forDataSource(
      ds = new slick.jdbc.DatabaseUrlDataSource {
        url = "postgres://backdoor_test_user:test@localhost:5432/backdoor_test_dummy"
      },
      maxConnections = Some(1),
      executor = slick.util.AsyncExecutor("dummyDb", 1, 1, 1, 1)
    )

    Await.result(
      awaitable = dummyDb
        .run {
          sql"SELECT 1 FROM pg_database WHERE datname = 'backdoor_test'".as[Int]
        }
        .flatMap { items =>
          if (items.isEmpty) {
            dbConfig.db.run { sqlu"DROP DATABASE backdoor_test;" }
          } else {
            Future(())
          }
        }
        .flatMap { _ =>
          dbConfig.db.run { sqlu"CREATE DATABASE bakcdoor_test;" }
        },
      atMost = Duration.apply(30, TimeUnit.SECONDS)
    )

    app.injector.instanceOf[EvolutionsApi].applyFor("default")
    dbConfig.db
  }
}

class BaseSpec extends FunSpec with BeforeAndAfter with BeforeAndAfterAll with MockitoSugar with Matchers {

  val app = BaseSpec.app
  val dbConfigProvider = BaseSpec.dbConfigProvider
  val dbConfig = BaseSpec.dbConfig
  val db = BaseSpec.db

  lazy implicit val controllerComponents = app.injector.instanceOf[ControllerComponents]
  lazy implicit val lang = play.api.i18n.Lang("en-US")

  lazy implicit val ec = scala.concurrent.ExecutionContext.Implicits.global
  lazy implicit val materializer = app.injector.instanceOf[Materializer]

  lazy implicit val config: PlayConfig = app.injector.instanceOf[PlayConfig]
  lazy implicit val system: ActorSystem = app.injector.instanceOf[ActorSystem]

  val idCol = Column("id", "bigint", "int8", false, true, Some("nextval('samples_id_seq'::regclass)"), None)
  val varcharCol = Column("varchar_field", "character varying", "varchar", true, false, None, Some(255))
  val textCol = Column("text_field", "text", "text", false, false, None, None)
  val textArrayCol = Column("text_array_field", "ARRAY", "_text", false, false, None, None)
  val int8ArrayCol = Column("int8_array_field", "ARRAY", "_int8", false, false, None, None)
  val jsonCol = Column("json_field", "jsonb", "jsonb", false, false, None, None)
  val hstoreCol = Column("hstore_field", "USER-DEFINED", "hstore", false, false, None, None)
  val charCol = Column("char_field", "character", "bpchar", false, false, None, Some(20))
  val bigintCol = Column("bigint_field", "bigint", "int8", false, false, None, None)
  val intCol = Column("int_field", "integer", "int4", false, false, None, None)
  val smallintCol = Column("smallint_field", "smallint", "int2", false, false, None, None)
  val booleanCol = Column("boolean_field", "boolean", "bool", false, false, None, None)
  val timestampCol = Column("timestamp_field", "timestamp without time zone", "timestamp", false, false, None, None)

  def i18n(key: String, args: Any*) = {
    val text = controllerComponents.messagesApi(key, args:_*)
    withClue(s"The message key '$key' is not defined in conf/messages for ${lang.code}.") {
      text should not be key
    }
    text
  }

  def await[T](f: Future[T], timeout: Duration = Duration.apply(15, TimeUnit.SECONDS)): T = {
    Await.result(f, timeout)
  }

  def resetDatabase(): Unit = {
    import dbConfig.profile.api._

    await(
      db.run {
        sql"SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename ASC;".as[String]
      }.flatMap { tables =>
        Future.sequence(
          tables.toList
            .filterNot(_ == "play_evolutions")
            .map { table =>
              db.run { sqlu"TRUNCATE #$table RESTART IDENTITY;" }
            }
        )
      }
    )
    app.injector.instanceOf[EvolutionsApi].applyFor("default")
  }

  def waitUntil(cond: => Boolean): Unit = {
    waitUntil(10, 500)(cond)
  }

  def waitUntil(timeoutInSeconds: Int, intervalInMs: Long)(cond: => Boolean): Unit = {
    var i = 1
    val maxIteration = Math.max(2, timeoutInSeconds * 1000 / intervalInMs)
    Thread.sleep(intervalInMs)
    while (i < maxIteration) {
      if (cond) {
        return
      }

      i += 1
      Thread.sleep(intervalInMs)
    }

    throw new TimeoutException()
  }

  def it2(name: String)(testFun: TimeControl => Any) = it(name) {
    Time.withCurrentTimeFrozen { tc =>
      testFun(tc)
    }
  }

  implicit def functionToArgumentMatcher[T](fn: T => Boolean): ArgumentMatcher[T] = {
    new ArgumentMatcher[T]() {
      override def matches(argument: scala.Any) = fn(argument.asInstanceOf[T])
    }
  }

  implicit def functionToAnswer[A, T](fn: A => T)(implicit tag: ClassTag[A]): Answer[T] = {
    new Answer[T] {
      override def answer(invocation: InvocationOnMock) = {
        fn(invocation.getArgumentAt[A](0, tag.runtimeClass.asInstanceOf[Class[A]]))
      }
    }
  }

  implicit def functionToAnswer[A, B, T](
    fn: A => B => T
  )(
    implicit tagA: ClassTag[A],
    tagB: ClassTag[B],
  ): Answer[T] = {
    new Answer[T] {
      override def answer(invocation: InvocationOnMock) = {
        fn(
          invocation.getArgumentAt[A](0, tagA.runtimeClass.asInstanceOf[Class[A]])
        )(
          invocation.getArgumentAt[B](1, tagA.runtimeClass.asInstanceOf[Class[B]])
        )
      }
    }
  }

  implicit def functionToAnswer[A, B, C, T](
    fn: (A, B, C) => T
  )(
    implicit tagA: ClassTag[A],
    tagB: ClassTag[B],
    tagC: ClassTag[C],
  ): Answer[T] = {
    new Answer[T] {
      override def answer(invocation: InvocationOnMock) = {
        fn(
          invocation.getArgumentAt[A](0, tagA.runtimeClass.asInstanceOf[Class[A]]),
          invocation.getArgumentAt[B](1, tagA.runtimeClass.asInstanceOf[Class[B]]),
          invocation.getArgumentAt[C](2, tagA.runtimeClass.asInstanceOf[Class[C]]),
        )
      }
    }
  }

  implicit def formToJsTuple[T](form: Form[T]): Seq[(String, JsValueWrapper)] = {
    form.data.mapValues { v => Json.toJsFieldJsValueWrapper(v) }.toSeq
  }

  def haveError(expected: String): Matcher[JsValue] = new Matcher[JsValue] {
    def apply(json: JsValue): MatchResult = {
      try {
        if ((json \ "success").as[Boolean]) {
          MatchResult(false, "'success' is not false.", "'success' is false as expected.")
        } else {
          val errors = (json \ "errors").as[Seq[JsValue]].map { error =>
            (error \ "message").as[String]
          }.toSet

          MatchResult(
            errors.size == 1 && errors.head == expected,
            s"$errors doesn't equal to $expected.",
            s"$errors equals to $expected as expected."
          )
        }
      } catch { case e: JsResultException =>
        MatchResult(
          false,
          s"${json.toString} is not in the expected format for failure.",
          s""
        )
      }
    }
  }

  def haveErrors(expected: String*): Matcher[JsValue] = new Matcher[JsValue] {
    def apply(json: JsValue): MatchResult = {
      try {
        if ((json \ "success").as[Boolean]) {
          MatchResult(false, "'success' is not false.", "'success' is false as expected.")
        } else {
          val errors = (json \ "errors").as[Seq[JsValue]].map { error =>
            (error \ "message").as[String]
          }.toSet
          val expectedSet = expected.toSet

          MatchResult(
            errors == expectedSet,
            s"$errors doesn't equal to $expectedSet.",
            s"$errors equals to $expectedSet as expected."
          )
        }
      } catch { case e: JsResultException =>
        MatchResult(
          false,
          s"${json.toString} is not in the expected format for failure.",
          s""
        )
      }
    }
  }
}
