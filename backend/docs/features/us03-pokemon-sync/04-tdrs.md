# US03 — Technical Decision Records

## TDR-001 — Synchronization endpoint shape

### Context / Problem

Phase 0 described the synchronization as `POST /api/v1/local-pokemon/{idOrName}`. US03 also adds `GET /api/v1/local-pokemon/{id}`, and US04 will add `PUT` and `DELETE` on the same path. In those routes the path variable is the **local** id.

### Options considered

#### Option A — `POST /api/v1/local-pokemon/{idOrName}`
- No request body; matches the Phase 0 wording.
- The same URL shape means "PokeAPI id or name" for `POST` and "local id" for every other method. `POST /local-pokemon/4` and `GET /local-pokemon/4` would refer to different Pokemon.

#### Option B — `POST /api/v1/local-pokemon` with body `{"idOrName": "..."}`
- Standard REST: `POST` to the collection creates a resource; `Location` points to the new local id.
- The path variable always means the local id.

#### Option C — `POST /api/v1/local-pokemon/sync/{idOrName}`
- No body, no ambiguity.
- A verb in the URL; less standard.

### Decision

Option B, approved by the user (it changes the Phase 0 wording).

### Rationale

One meaning per URL shape avoids mistakes for API clients and in the frontend.

### Consequences

- The request body is validated with Bean Validation, like the auth requests.
- The future "create own Pokemon" (backlog item 2) can use the same `POST` with a different body.

---

## TDR-002 — Transaction boundary during synchronization

### Context / Problem

Synchronization calls PokeAPI (up to 5 s read timeout) and then writes to PostgreSQL. A `@Transactional` use case would hold a database connection during the HTTP call.

### Options considered

#### Option A — `@Transactional` on the whole use case
- Simple to read.
- A slow PokeAPI keeps a pooled database connection busy; with several slow requests the pool can run out.

#### Option B — PokeAPI call outside the transaction; short transaction only for the insert
- No connection held during the HTTP call.
- The "already local" check and the insert are not in the same transaction, so a race between two requests is possible; the unique constraint on `poke_api_id` catches it and the adapter turns it into the same `409`.

### Decision

Option B.

### Rationale

External calls should not run inside database transactions. The unique constraint already gives the correctness guarantee, so the race is harmless.

### Consequences

- The use case has no `@Transactional`; the adapter's `insert` uses `saveAndFlush` (its own transaction) and translates `DataIntegrityViolationException`.
