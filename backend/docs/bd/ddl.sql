-- Consolidated schema. Source of truth: Liquibase changesets in src/main/resources/db/changelog/changes.
-- Keep this file in sync with them (see bddr.md).

-- Users of the API (user-auth feature).
CREATE TABLE app_user (
    id            BIGINT GENERATED ALWAYS AS IDENTITY,
    username      VARCHAR(30)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_username_lower CHECK (username = lower(username))
);
