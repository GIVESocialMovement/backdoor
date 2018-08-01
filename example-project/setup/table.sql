
DROP TABLE IF EXISTS "examples";

CREATE EXTENSION IF NOT EXISTS hstore;

CREATE TABLE "examples" (
  "id" BIGSERIAL PRIMARY KEY,
  "email" varchar(255) NOT NULL,
  "description" TEXT,
  "level" INT NOT NULL,
  "tags" HSTORE NOT NULL,
  "categories" varchar(255)[] NOT NULL,
  "form" JSON,
  "birthdate" TIMESTAMP NOT NULL
);
