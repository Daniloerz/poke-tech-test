# US03 — Architecture Decision Records

## ADR-001 — Separate domain model for the local copy

### Context / Problem

The catalog already has a `Pokemon` domain record (US01, US02), read from PokeAPI and cached in Redis. The local copy has a different life: it is stored in PostgreSQL, has its own id, proprietary fields and audit times, and it is edited by users (US04). We must decide whether both use the same model.

### Options considered

#### Option A — Reuse `Pokemon` and add the local fields to it
- One model.
- The catalog model would carry fields that PokeAPI never has (`localizedName`, `tags`, local `id`, `syncedAt`), and the cached JSON would change every time the local model changes. Two different lifecycles mixed in one type.

#### Option B — New `LocalPokemon` model, created from a `Pokemon` snapshot
- `LocalPokemon.fromCatalog(pokemon)` copies the fields we keep; the proprietary fields only exist in `LocalPokemon`.
- Two models with some similar fields (name, types, measures).

### Decision

Option B.

### Rationale

The catalog model is a read-only view of an external system; the local model is our own data, with its own rules and lifecycle. Keeping them apart means a change in one cannot break the other, and the "snapshot" idea is explicit in the code.

### Consequences

- The shared rules (unit conversion, identifier normalization) live in small domain helpers used by both models, so they are not duplicated.
- The REST API also has two resources: `/api/v1/pokemon` (catalog, public) and `/api/v1/local-pokemon` (local data, protected).
