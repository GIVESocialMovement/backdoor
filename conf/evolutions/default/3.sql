# History

# --- !Ups

CREATE TABLE "history_entries" (
    "id" BIGSERIAL PRIMARY KEY,
    "user_id" BIGINT NOT NULL,
    "original_record" JSON NOT NULL,
    "sql" TEXT NOT NULL,
    "table" VARCHAR(255) NOT NULL,
    "performed_at" BIGINT NOT NULL
);

# --- !Downs

DROP TABLE "history_entries" CASCADE;
