package givers.backdoor.framework.services

import givers.backdoor.Permission.Scope
import givers.backdoor.framework.models.{TableModel, User}
import givers.backdoor.{Permission, Permissions}
import helpers.BaseSpec

class AccessServiceSpec extends BaseSpec {

  val users = Seq(
    User(1L, "test@give.asia", ""),
    User(2L, "test2@give.asia", ""),
    User(3L, "test3@give.asia", ""),
    User(4L, "test4@give.asia", ""),
  )


  it("tests hasAccess") {
    val permissions = Map(
      users.head.email -> Permission(perColumn = Map("*" -> Map("*" -> Scope.Read)))
    )
    val service = new AccessService(
      new Permissions {
        override def get(email: String) = permissions.get(email)
      }
    )
    service.hasAccess(users.head) should be(true)
    service.hasAccess(users(1)) should be(false)
  }

  it("tests canCreate") {
    val permissions = Map(
      users.head.email -> Permission(create = Set("*")),
      users(1).email -> Permission(create = Set("samples")),
      users(2).email -> Permission(create = Set("samples", "*"))
    )

    val service = new AccessService(
      new Permissions {
        override def get(email: String) = permissions.get(email)
      }
    )
    service.canCreate(users.head, TableModel("samples")) should be(true)
    service.canCreate(users.head, TableModel("random_table")) should be(true)

    service.canCreate(users(1), TableModel("samples")) should be(true)
    service.canCreate(users(1), TableModel("random_table")) should be(false)

    service.canCreate(users(2), TableModel("samples")) should be(true)
    service.canCreate(users(2), TableModel("random_table")) should be(true)

    service.canCreate(users(3), TableModel("samples")) should be(false)
    service.canCreate(users(3), TableModel("random_table")) should be(false)
  }

  it("tests canDelete") {
    val permissions = Map(
      users.head.email -> Permission(delete = Set("*")),
      users(1).email -> Permission(delete = Set("samples")),
      users(2).email -> Permission(delete = Set("samples", "*"))
    )

    val service = new AccessService(
      new Permissions {
        override def get(email: String) = permissions.get(email)
      }
    )

    service.canDelete(users.head, TableModel("samples")) should be(true)
    service.canDelete(users.head, TableModel("random_table")) should be(true)

    service.canDelete(users(1), TableModel("samples")) should be(true)
    service.canDelete(users(1), TableModel("random_table")) should be(false)

    service.canDelete(users(2), TableModel("samples")) should be(true)
    service.canDelete(users(2), TableModel("random_table")) should be(true)

    service.canDelete(users(3), TableModel("samples")) should be(false)
    service.canDelete(users(3), TableModel("random_table")) should be(false)
  }

  it("get scope for column") {
    val permissions = Map(
      users.head.email -> Permission(perColumn = Map("*" -> Map("*" -> Scope.Read))),
      users(1).email -> Permission(perColumn = Map("*" -> Map(textCol.name -> Scope.Read, jsonCol.name -> Scope.Write), "samples" -> Map("*" -> Scope.Write))),
      users(2).email -> Permission(perColumn = Map("samples" -> Map(textCol.name -> Scope.Read, idCol.name -> Scope.Write)))
    )

    val service = new AccessService(
      new Permissions {
        override def get(email: String) = permissions.get(email)
      }
    )

    service.getScope(users.head, TableModel("samples"), idCol) should be(Scope.Read)
    service.getScope(users.head, TableModel("random_tables"), hstoreCol) should be(Scope.Read)

    service.getScope(users(1), TableModel("samples"), jsonCol) should be(Scope.Write)
    service.getScope(users(1), TableModel("samples"), textCol) should be(Scope.Write)
    service.getScope(users(1), TableModel("random_tables"), textCol) should be(Scope.Read)
    service.getScope(users(1), TableModel("random_tables"), jsonCol) should be(Scope.Write)
    service.getScope(users(1), TableModel("random_tables"), idCol) should be(Scope.None)
    service.getScope(users(1), TableModel("random_tables"), intCol) should be(Scope.None)

    service.getScope(users(2), TableModel("samples"), textCol) should be(Scope.Read)
    service.getScope(users(2), TableModel("samples"), idCol) should be(Scope.Write)
    service.getScope(users(2), TableModel("samples"), jsonCol) should be(Scope.None)
    service.getScope(users(2), TableModel("samples"), intCol) should be(Scope.None)
    service.getScope(users(2), TableModel("random_tables"), textCol) should be(Scope.None)
    service.getScope(users(2), TableModel("random_tables"), jsonCol) should be(Scope.None)
  }
}
