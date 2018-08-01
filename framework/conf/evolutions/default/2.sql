# Users schema

# --- !Ups

BEGIN;

CREATE TABLE "users" (
  "id" BIGSERIAL PRIMARY KEY,
  "email" varchar(255) NOT NULL,
  "cookie_secret" varchar(255) NOT NULL
);

COMMIT;

# --- !Downs

DROP TABLE "users" CASCADE;

