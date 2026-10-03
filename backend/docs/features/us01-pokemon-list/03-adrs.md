# US01 — Architecture Decision Records

## ADR-001 — Package structure and layers

### Context / Problem

The exercise asks for Clean Architecture: the business logic must be independent from the API and from the data access. We need a structure that shows this clearly in an interview, without adding layers that have no real job.

### Options considered

#### Option A — Classic layers (`controller`, `service`, `repository`)
- Simple and well known.
- Services usually depend directly on JPA entities and HTTP clients, so the business logic is coupled to infrastructure. It does not show Clean Architecture.

#### Option B — Full hexagonal (in ports + out ports for every use case)
- Every use case has an input interface and an implementation; every external system has an output interface and an adapter.
- Input interfaces have only one implementation and only one caller (the controller). They add files and indirection without decoupling anything real.

#### Option C — Pragmatic hexagonal (out ports only)
- Layers `domain`, `application`, `infrastructure`, `interfaces`. Dependencies point inwards.
- **Out ports** (interfaces in `application/port/out`) for external systems: PokeAPI now, the database later. Adapters in `infrastructure` implement them.
- Use cases are concrete classes. Controllers call them directly.

### Decision

Option C.

### Rationale

Out ports give real decoupling: the use cases are tested with mocks of the port, and PokeAPI or PostgreSQL can be replaced without changing the use case. Input ports would only duplicate the use case signature. The structure is easy to explain: "the inner layers do not know the outer ones".

### Consequences

- `domain` has no Spring, JPA, Jackson or HTTP code.
- `application` uses only `@Service` (and later `@Transactional`) from Spring, as a pragmatic exception: creating the beans by hand would add configuration without real benefit.
- DTOs of PokeAPI and of the REST API never enter the domain. Mappers live in the outer layers.
- If a use case later needs several implementations, an input port can be added then.

---

## ADR-002 — Where to cache PokeAPI responses

### Context / Problem

One page of the list needs 1 + N calls to PokeAPI. Without a cache, every request repeats all those calls. The exercise asks for a caching layer for PokeAPI responses. We have to decide what to cache and in which layer.

### Options considered

#### Option A — Cache the final REST response (controller or use case level)
- One cache entry per `(page, size)`.
- Pages with different `size` do not share data, and US02 cannot reuse anything. The use case would also depend on caching details.

#### Option B — Cache each PokeAPI call in the adapter
- Two caches: list pages (`offset`, `limit`) and single Pokemon (`name`).
- Each Pokemon is stored once and reused by every page and by US02. The cache is an infrastructure detail; the use case does not know it exists.
- Two cache reads per Pokemon of a page instead of one.

### Decision

Option B, with Redis as the shared cache (as the exercise and the stack require).

### Rationale

It caches exactly what the exercise asks for (PokeAPI responses), gives the best reuse between pages and features, and keeps the application layer clean.

### Consequences

- `@Cacheable` lives on the adapter methods. Calls come from the use case through the Spring proxy, so the cache works (no self-invocation).
- The cached values are domain objects, so they must be serializable to JSON (TDR-003).
- If Redis fails, the application must keep working (AC-11).
