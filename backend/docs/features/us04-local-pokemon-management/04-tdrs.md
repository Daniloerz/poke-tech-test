# US04 — Technical Decision Records

## TDR-001 — `PUT` or `PATCH` for the update

### Context / Problem

Only three fields are editable (`localizedName`, `region`, `tags`). We need an update operation that is simple to use and to validate.

### Options considered

#### Option A — `PATCH` with partial updates (JSON Merge Patch)
- The client sends only what changes.
- "Missing" and "null" mean different things (keep vs clear), which needs extra code (`Optional` wrappers or `JsonNullable`) and more tests.

#### Option B — `PUT` replacing the whole editable part
- The client sends the three fields; `null` clears a text field and `[]` clears the tags.
- Idempotent and easy to validate. The client must send all three fields (it already has them from `GET`).

### Decision

Option B.

### Rationale

With only three small fields, a full replacement is simpler and removes the "missing vs null" ambiguity.

### Consequences

- The snapshot fields are not part of `PUT`; they are rejected if sent (TDR-002).

---

## TDR-002 — Strict request body (unknown fields)

### Context / Problem

By default Spring Boot ignores unknown JSON fields. A client sending `{"name": "Raichu"}` to `PUT` would get `200` while nothing changed.

### Options considered

#### Option A — Ignore unknown fields (default)
- Tolerant to new client versions.
- Hides mistakes; the client believes the snapshot was changed.

#### Option B — Fail on unknown fields globally (`spring.jackson` setting)
- Strict everywhere.
- Changes the behaviour of every endpoint, including auth, as a side effect of this feature.

#### Option C — Fail on unknown fields only in `UpdateLocalPokemonRequest`
- `@JsonIgnoreProperties(ignoreUnknown = false)` on the record; the `400` names the field.
- Scope limited to the endpoint where the mistake matters.
- **Does not work:** `ignoreUnknown = false` only means "do not ignore by this annotation"; it does not override the global setting of Spring Boot, which ignores unknown fields. The test `updateRejectsSnapshotFieldsNamingTheField` returned `200`.

### Decision

Option B: `spring.jackson.deserialization.fail-on-unknown-properties: true`. (The plan first chose Option C; the implementation showed that it cannot work, so the decision was changed.)

### Rationale

It is the "further defensive logic" the story asks for, with one line of configuration. Being strict in every request body is also consistent: a client that sends `{"role": "admin"}` to register gets a clear `400` instead of a silent success.

### Consequences

- Every request body (register, login, sync, update) rejects unknown fields with `400`, `errors: [{field, "is not a recognized field"}]`.
- It only affects how the API reads requests. The PokeAPI client has its own `RestClient` and its DTOs ignore unknown fields explicitly, so PokeAPI can add fields without breaking us (checked with Docker Compose).

---

## TDR-003 — One generic page model

### Context / Problem

US01 has `PokemonPage` for the catalog. US04 needs the same page data for local Pokemon.

### Options considered

#### Option A — A second record `LocalPokemonPage`
- No change to US01.
- Duplicates the page rules (`totalPages`, validation).

#### Option B — Generic `PageResult<T>` replacing `PokemonPage`
- One page model and one test; the REST layer already has a generic `PageResponse<T>`.
- Small refactor of US01 code and tests.

### Decision

Option B.

### Rationale

Same rule in one place; the refactor is small and covered by the existing tests.

### Consequences

- `ListPokemonUseCase` returns `PageResult<Pokemon>`; `PokemonPageTest` becomes `PageResultTest`.
