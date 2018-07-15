package givers.backdoor.framework.services

import givers.backdoor.ComputedColumns
import givers.backdoor.framework.libraries.{AuthenticatedContext, PageContext}
import givers.backdoor.framework.models.{AccessibleTable, TableModel, User}
import helpers.BaseSpec
import org.mockito.Matchers._
import org.mockito.Mockito._

import scala.concurrent.Future

class AccessibleTableServiceSpec extends BaseSpec {
  var tableService: TableService = _
  var accessService: AccessService = _
  var computedColumns: ComputedColumns = _
  var service: AccessibleTableService = _

  val context = AuthenticatedContext(
    loggedInUser = User(1L, "email", "secret"),
    request = null,
    config = config,
    page = PageContext()
  )
  val table = TableModel("samples")

  before {
    tableService = mock[TableService]
    accessService = mock[AccessService]
    computedColumns = mock[ComputedColumns]
    service = new AccessibleTableService(tableService, accessService, computedColumns)
  }

  describe("gets table") {
    it("no access") {
      when(tableService.getTable(any())).thenReturn(Future(Some(table)))
      when(accessService.canRead(any(), any())).thenReturn(false)
      when(accessService.canCreate(any(), any())).thenReturn(false)
      when(accessService.canDelete(any(), any())).thenReturn(false)

      await(service.getTable("samples")(context)) should be(None)

      verify(tableService).getTable("samples")
      verify(accessService).canRead(context.loggedInUser, table)
      verify(accessService).canCreate(context.loggedInUser, table)
      verify(accessService).canDelete(context.loggedInUser, table)
    }

    it("has access") {
      when(tableService.getTable(any())).thenReturn(Future(Some(table)))
      when(accessService.canRead(any(), any())).thenReturn(true)
      when(accessService.canCreate(any(), any())).thenReturn(false)
      when(accessService.canDelete(any(), any())).thenReturn(true)

      await(service.getTable("samples")(context)) should be(Some(AccessibleTable(table, canRead = true, canCreate = false, canDelete = true)))

      verify(tableService).getTable("samples")
      verify(accessService).canRead(context.loggedInUser, table)
      verify(accessService).canCreate(context.loggedInUser, table)
      verify(accessService).canDelete(context.loggedInUser, table)
    }
  }

  it("lists tables") {
    val anotherTable = TableModel("no_access")
    when(tableService.getTables()).thenReturn(Future(Seq(table, anotherTable)))
    when(accessService.canRead(any(), any())).thenReturn(true).thenReturn(false)
    when(accessService.canCreate(any(), any())).thenReturn(true).thenReturn(true)
    when(accessService.canDelete(any(), any())).thenReturn(false).thenReturn(false)

    await(service.getTables()(context)) should be(Seq(AccessibleTable(table, canRead = true, canCreate = true, canDelete = false)))

    verify(tableService).getTables()
    verify(accessService).canRead(context.loggedInUser, table)
    verify(accessService).canCreate(context.loggedInUser, table)
    verify(accessService).canDelete(context.loggedInUser, table)
    verify(accessService).canRead(context.loggedInUser, anotherTable)
    verify(accessService).canCreate(context.loggedInUser, anotherTable)
    verify(accessService).canRead(context.loggedInUser, anotherTable)
  }
}
