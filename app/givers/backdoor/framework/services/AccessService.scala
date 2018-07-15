package givers.backdoor.framework.services

import com.google.inject.{Inject, Singleton}
import givers.backdoor.Permission.Scope
import givers.backdoor.Permissions
import givers.backdoor.framework.models.{BaseColumn, TableModel, User}

@Singleton
class AccessService @Inject()(
  permissions: Permissions
) {
  def hasAccess(user: User): Boolean = {
    permissions.get(user.email.toLowerCase).isDefined
  }

  private[this] def getColumns(perColumn: Map[String, Map[String, Scope.Value]], table: TableModel): Option[Map[String, Scope.Value]] = {
    perColumn.get(table.name).orElse(perColumn.get("*"))
  }

  def getScope(user: User, table: TableModel, column: BaseColumn): Scope.Value = {
    val resultOpt = for {
      perColumn <- permissions.get(user.email).map(_.perColumn)
      columns <- getColumns(perColumn, table)
      permission <- columns.get(column.name).orElse(columns.get("*"))
    } yield {
      permission
    }

    resultOpt.getOrElse(Scope.None)
  }

  def canRead(user: User, table: TableModel): Boolean = {
    permissions.get(user.email).exists { permission =>
      getColumns(permission.perColumn, table).isDefined
    }
  }

  def canDelete(user: User, table: TableModel): Boolean = {
    permissions.get(user.email).exists { permission =>
      permission.delete.contains("*") || permission.delete.contains(table.name)
    }
  }

  def canCreate(user: User, table: TableModel): Boolean = {
    permissions.get(user.email).exists { permission =>
      permission.create.contains("*") || permission.create.contains(table.name)
    }
  }
}
