package givers.backdoor.testproject

import givers.backdoor.Permission
import givers.backdoor.Permission.Scope


class Permissions extends givers.backdoor.Permissions {
  val PERMISSIONS = Map(
    "backdoor.test.user@gmail.com" -> Permission(
      create = Set("*"),
      delete = Set("*"),
      perColumn = Map("*" -> Map("*" -> Scope.Write))
    )
  )

  def get(email: String): Option[Permission] = PERMISSIONS.get(email)
}