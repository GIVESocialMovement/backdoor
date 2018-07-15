package givers.backdoor.framework.services

import java.sql.Timestamp

import givers.backdoor.framework.models.Sort.Direction
import givers.backdoor.framework.models._
import helpers.BaseSpec
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.libs.json.Json
import slick.jdbc.PostgresProfile

import scala.concurrent.Future

class TableServiceSpec extends BaseSpec {

  var targetDb: PostgresProfile.backend.DatabaseDef = _
  var webhookService: WebhookService = _
  var tableService: TableService = _
  val user = User(1L, "test@test.com", "secret")

  val allCols = Seq(
    idCol,
    varcharCol,
    textCol,
    textArrayCol,
    int8ArrayCol,
    jsonCol,
    hstoreCol,
    charCol,
    bigintCol,
    intCol,
    smallintCol,
    booleanCol,
    timestampCol
  )

  before {
    resetDatabase()
    import slick.jdbc.PostgresProfile.api._
    webhookService = mock[WebhookService]
    tableService = new TableService(app.injector.instanceOf[HistoryEntryService], webhookService, config)

    when(webhookService.process(any(), any(), any())).thenReturn(Future(()))

    val tables = await(tableService.db.run {
      sql"SELECT tablename FROM pg_tables WHERE schemaname='public' ORDER BY tablename ASC;".as[String]
    })

    tables.foreach { table =>
      await(tableService.db.run { sqlu"DROP TABLE IF EXISTS #$table CASCADE;" })
    }

    await(tableService.db.run {
      sqlu"""CREATE EXTENSION IF NOT EXISTS HSTORE;"""
    })

    await(tableService.db.run {
      sqlu"""CREATE TABLE "samples" (#${allCols.map(_.getSpecForCreateTable).mkString(",")});"""
    })

    await(tableService.insert(
      table = "samples",
      fields = Seq(
        OptionalField(None, varcharCol),
        Field(StringValue("text-value"), textCol),
        SeqField(Seq(StringValue("a"), StringValue("b")), textArrayCol),
        SeqField(Seq(LongValue(Long.MaxValue), LongValue(1000L)), int8ArrayCol),
        Field(JsonValue(Json.obj("k" -> "v")), jsonCol),
        Field(HStoreValue(Map("a" -> "b")), hstoreCol),
        Field(StringValue("char-value"), charCol),
        Field(LongValue(1234L), bigintCol),
        Field(IntValue(123), intCol),
        Field(IntValue(12), smallintCol),
        Field(BooleanValue(true), booleanCol),
        Field(TimestampValue(new Timestamp(0L)), timestampCol)
      ),
      committer = user
    ))
    await(tableService.insert(
      table = "samples",
      fields = Seq(
        OptionalField(Some(StringValue("varchar-col")), varcharCol),
        Field(StringValue("text-value-2"), textCol),
        SeqField(Seq(StringValue("love"), StringValue("honesty")), textArrayCol),
        SeqField(Seq(LongValue(100L)), int8ArrayCol),
        Field(JsonValue(Json.obj("kv" -> "vvv")), jsonCol),
        Field(HStoreValue(Map("aa" -> "bb")), hstoreCol),
        Field(StringValue("char-value-2"), charCol),
        Field(LongValue(999L), bigintCol),
        Field(IntValue(99), intCol),
        Field(IntValue(9), smallintCol),
        Field(BooleanValue(false), booleanCol),
        Field(TimestampValue(Timestamp.valueOf("2018-07-04 01:02:03")), timestampCol)
      ),
      committer = user
    ))
  }

  it("gets") {
    val rows = await(tableService.getRows("samples", allCols, Seq.empty, Seq(Sort(idCol, Direction.Asc)), 0, Int.MaxValue))
    rows.size should be(2)

    val first = rows.head
    val second = rows(1)

    val firstIdField = first.fields.head.asInstanceOf[Field[LongValue]]
    firstIdField.value should be(LongValue(1L))
    first.fields(1).asInstanceOf[OptionalField[StringValue]].valueOpt should be(None)
    first.fields(2).asInstanceOf[Field[StringValue]].value should be(StringValue("text-value"))
    first.fields(3).asInstanceOf[SeqField[StringValue]].values should be(Seq(StringValue("a"), StringValue("b")))
    first.fields(4).asInstanceOf[SeqField[LongValue]].values should be(Seq(LongValue(Long.MaxValue), LongValue(1000L)))
    first.fields(5).asInstanceOf[Field[JsonValue]].value should be(JsonValue(Json.obj("k" -> "v")))
    first.fields(6).asInstanceOf[Field[HStoreValue]].value should be(HStoreValue(Map("a" -> "b")))
    // Postgresql pads char(20) with spaces when the content doesn't reach 20 characters.
    first.fields(7).asInstanceOf[Field[StringValue]].value should be(StringValue("char-value          "))
    first.fields(8).asInstanceOf[Field[LongValue]].value should be(LongValue(1234L))
    first.fields(9).asInstanceOf[Field[IntValue]].value should be(IntValue(123))
    first.fields(10).asInstanceOf[Field[IntValue]].value should be(IntValue(12))
    first.fields(11).asInstanceOf[Field[BooleanValue]].value should be(BooleanValue(true))
    first.fields(12).asInstanceOf[Field[TimestampValue]].value should be(TimestampValue(new Timestamp(0L)))

    second.fields.head.asInstanceOf[Field[LongValue]].value should be(LongValue(2L))
    second.fields(1).asInstanceOf[OptionalField[StringValue]].valueOpt should be(Some(StringValue("varchar-col")))
    second.fields(2).asInstanceOf[Field[StringValue]].value should be(StringValue("text-value-2"))
    second.fields(3).asInstanceOf[SeqField[StringValue]].values should be(Seq(StringValue("love"), StringValue("honesty")))
    second.fields(4).asInstanceOf[SeqField[LongValue]].values should be(Seq(LongValue(100L)))
    second.fields(5).asInstanceOf[Field[JsonValue]].value should be(JsonValue(Json.obj("kv" -> "vvv")))
    second.fields(6).asInstanceOf[Field[HStoreValue]].value should be(HStoreValue(Map("aa" -> "bb")))
    second.fields(7).asInstanceOf[Field[StringValue]].value should be(StringValue("char-value-2        "))
    second.fields(8).asInstanceOf[Field[LongValue]].value should be(LongValue(999L))
    second.fields(9).asInstanceOf[Field[IntValue]].value should be(IntValue(99))
    second.fields(10).asInstanceOf[Field[IntValue]].value should be(IntValue(9))
    second.fields(11).asInstanceOf[Field[BooleanValue]].value should be(BooleanValue(false))
    second.fields(12).asInstanceOf[Field[TimestampValue]].value should be(TimestampValue(Timestamp.valueOf("2018-07-04 01:02:03")))
  }

  it("updates") {
    val rows = await(tableService.getRows("samples", allCols, Seq.empty, Seq(Sort(idCol, Direction.Asc)), 0, Int.MaxValue))
    rows.size should be(2)

    val first = rows.head
    val second = rows(1)
    val firstIdField = first.fields.head.asInstanceOf[Field[LongValue]]

    await(tableService.update(
      table = "samples",
      field = Field(StringValue("updated-text-value"), textCol),
      primaryKey = firstIdField,
      committer = user
    ))
    await(tableService.update(
      table = "samples",
      field = Field(TimestampValue(new Timestamp(1L)), timestampCol),
      primaryKey = firstIdField,
      committer = user
    ))

    val updatedRows = await(tableService.getRows("samples", allCols, Seq.empty, Seq(Sort(idCol, Direction.Asc)), 0, Int.MaxValue))
    updatedRows.size should be(2)

    val updatedFirst = updatedRows.head
    val updatedSecond = updatedRows(1)

    updatedFirst.fields(2).asInstanceOf[Field[StringValue]].value should be(StringValue("updated-text-value"))
    updatedFirst.fields(12).asInstanceOf[Field[TimestampValue]].value should be(TimestampValue(new Timestamp(1L)))

    updatedFirst.fields.head should be(first.fields.head)
    updatedFirst.fields(1) should be(first.fields(1))
    updatedFirst.fields(3) should be(first.fields(3))
    updatedFirst.fields(4) should be(first.fields(4))
    updatedFirst.fields(5) should be(first.fields(5))
    updatedFirst.fields(6) should be(first.fields(6))
    updatedFirst.fields(7) should be(first.fields(7))
    updatedFirst.fields(8) should be(first.fields(8))
    updatedFirst.fields(9) should be(first.fields(9))
    updatedFirst.fields(10) should be(first.fields(10))
    updatedFirst.fields(11) should be(first.fields(11))

    second should be(updatedSecond)
  }

  it("deletes") {
    val rows = await(tableService.getRows("samples", allCols, Seq.empty, Seq(Sort(idCol, Direction.Asc)), 0, Int.MaxValue))
    rows.size should be(2)

    val first = rows.head
    val second = rows(1)
    val firstIdField = first.fields.head.asInstanceOf[Field[LongValue]]

    await(tableService.delete(
      table = "samples",
      primaryKey = firstIdField,
      committer = user
    ))

    val updatedRows = await(tableService.getRows("samples", allCols, Seq.empty, Seq(Sort(idCol, Direction.Asc)), 0, Int.MaxValue))
    updatedRows.size should be(1)

    updatedRows.head should be(second)
  }

  it("get column") {
    await(tableService.getColumn("samples", hstoreCol.name)) should be(Some(hstoreCol))
    await(tableService.getColumn("samples", "not_existing_column")) should be(None)
  }

  it("get field") {
    val rows = await(tableService.getRows("samples", allCols, Seq.empty, Seq(Sort(idCol, Direction.Asc)), 0, Int.MaxValue))
    rows.size should be(2)

    val first = rows.head
    val firstIdField = first.fields.head.asInstanceOf[Field[LongValue]]

    await(tableService.get("samples", hstoreCol, idCol, firstIdField.value.value.toString)) should be(
      Some(Field(
        HStoreValue(Map("a" -> "b")),
        hstoreCol
      ))
    )
  }

  it("gets total count") {
    await(tableService.getTotalCount("samples", Seq.empty)) should be(2)
  }

  it("get tables") {
    await(tableService.getTables()) should be(Seq(TableModel("samples")))
  }

  it("get table") {
    await(tableService.getTable("samples")) should be(Some(TableModel("samples")))
    await(tableService.getTable("samples-1")) should be(None)
  }

  it("get columns") {
    val columns = await(tableService.getColumns("samples"))
    columns should be(Seq(
      idCol,
      varcharCol,
      textCol,
      textArrayCol,
      int8ArrayCol,
      jsonCol,
      hstoreCol,
      charCol,
      bigintCol,
      intCol,
      smallintCol,
      booleanCol,
      timestampCol
    ))
  }
}
