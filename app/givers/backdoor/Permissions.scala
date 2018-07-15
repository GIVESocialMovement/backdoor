package givers.backdoor

object Permission {
  object Scope {
    sealed abstract class Value {
      def isAccessible: Boolean

      def canRead: Boolean

      def canWrite: Boolean
    }

    object Read extends Value {
      val isAccessible = true
      val canRead = true
      val canWrite = false
    }

    object Write extends Value {
      val isAccessible = true
      val canRead = true
      val canWrite = true
    }

    object None extends Value {
      val isAccessible = false
      val canRead = false
      val canWrite = false
    }
  }
}

abstract class Permissions {
  def get(email: String): Option[Permission]
}

case class Permission(
  create: Set[String] = Set.empty,
  delete: Set[String] = Set.empty,
  perColumn: Map[String, Map[String, Permission.Scope.Value]] = Map.empty
)
