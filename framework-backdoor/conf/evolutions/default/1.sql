# Users schema

# --- !Ups

BEGIN;

CREATE TABLE "login_callbacks" (
    "id" BIGSERIAL PRIMARY KEY,
    "secret_key" varchar(255) NOT NULL,
    "origin_url" varchar(255) NOT NULL,
    "status" varchar(255) NOT NULL,
    "created_at" BIGINT NOT NULL
);

COMMIT;

# --- !Downs

DROP TABLE IF EXISTS "login_callbacks" CASCADE;
