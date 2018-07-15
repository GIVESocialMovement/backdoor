package givers.backdoor.framework.models

import helpers.BaseSpec

class ColumnSpec extends BaseSpec {

  describe("getCanonicalType") {
    it("gets bigint") {
      bigintCol.getCanonicalType should be("int8")
    }

    it("gets varchar") {
      varcharCol.getCanonicalType should be(s"varchar(${varcharCol.charMaxLengthOpt.get})")
    }

    it("gets text") {
      textCol.getCanonicalType should be("text")
    }

    it("gets text[]") {
      textArrayCol.getCanonicalType should be("text[]")
    }

    it("gets int8[]") {
      int8ArrayCol.getCanonicalType should be("int8[]")
    }

    it("gets json") {
      jsonCol.getCanonicalType should be("jsonb")
    }

    it("gets hstore") {
      hstoreCol.getCanonicalType should be("hstore")
    }

    it("gets character") {
      charCol.getCanonicalType should be(s"bpchar(${charCol.charMaxLengthOpt.get})")
    }

    it("gets bigserial") {
      idCol.getCanonicalType should be("bigserial")
    }

    it("gets integer") {
      intCol.getCanonicalType should be("int4")
    }

    it("gets smallint") {
      smallintCol.getCanonicalType should be("int2")
    }

    it("gets boolean") {
      booleanCol.getCanonicalType should be("bool")
    }

    it("gets timestamp") {
      timestampCol.getCanonicalType should be("timestamp")
    }
  }
}
