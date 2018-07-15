package givers.backdoor.framework.libraries

import com.twitter.util.Time
import slick.jdbc.PostgresProfile.api._

object ExtraPostgresColumnType {

  def timeToSqlType(time: Time) = time.inMillis

  implicit val timeColumnType = MappedColumnType.base[Time, Long](
    timeToSqlType, { millis => Time.fromMilliseconds(millis) })
}
