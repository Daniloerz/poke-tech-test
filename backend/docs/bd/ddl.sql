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

-- Local, editable copy of PokeAPI Pokemon with proprietary fields (US03, US04).
CREATE TABLE local_pokemon (
    id                BIGINT GENERATED ALWAYS AS IDENTITY,
    poke_api_id       INTEGER      NOT NULL,
    name              VARCHAR(100) NOT NULL,
    sprite_url        VARCHAR(500),
    types             TEXT[]       NOT NULL DEFAULT '{}',
    height_decimetres INTEGER      NOT NULL,
    weight_hectograms INTEGER      NOT NULL,
    localized_name    VARCHAR(100),
    region            VARCHAR(100),
    tags              TEXT[]       NOT NULL DEFAULT '{}',
    synced_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_local_pokemon PRIMARY KEY (id),
    CONSTRAINT uk_local_pokemon_poke_api_id UNIQUE (poke_api_id),
    CONSTRAINT ck_local_pokemon_poke_api_id_positive CHECK (poke_api_id > 0),
    CONSTRAINT ck_local_pokemon_measures_not_negative CHECK (height_decimetres >= 0 AND weight_hectograms >= 0)
);
