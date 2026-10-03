# US01 — Technical Decision Records

## TDR-001 — HTTP client for PokeAPI

### Context / Problem

The backend calls PokeAPI with simple GET requests. We need an HTTP client that fits Spring Boot 4.1 and the blocking Spring MVC stack.

### Options considered

#### Option A — `RestClient`
- Synchronous, fluent API, included in Spring Framework. Configured with a base URL and timeouts.
- Works well with MVC and with virtual threads. Easy to test with `MockRestServiceServer`.

#### Option B — HTTP interface (`@HttpExchange`) on top of `RestClient`
- Declarative interface; Spring creates the implementation.
- Less code per endpoint, but more "magic" for only two endpoints, and error handling is less explicit.

#### Option C — `WebClient` or OpenFeign
- `WebClient` needs WebFlux (reactive) only to do blocking calls. Feign is an extra Spring Cloud dependency.

### Decision

Option A, `RestClient`.

### Rationale

It is the standard client for blocking code in current Spring. It needs no extra dependency, and the request and error handling are explicit and easy to explain.

### Consequences

- Timeouts are set on the request factory from configuration.
- `RestClientException` is translated to a business exception in the adapter.

---

## TDR-002 — Parallel calls to PokeAPI

### Context / Problem

For each page we fetch N Pokemon (up to 50). Sequential calls would take N times the latency. We need concurrency that is simple, bounded and keeps the order.

### Options considered

#### Option A — `parallelStream()`
- Very short code.
- Uses the common `ForkJoinPool`, sized for CPU work. Blocking I/O there blocks threads shared with the whole JVM.

#### Option B — `CompletableFuture` with a fixed thread pool
- Explicit control. The pool size limits the concurrency.
- Pool size must be tuned; platform threads are blocked while waiting for I/O.

#### Option C — `CompletableFuture` with a virtual-thread executor (Java 21)
- Each call runs on a cheap virtual thread; waiting for I/O does not block a platform thread.
- The concurrency per request is already limited by `size <= 50`.

### Decision

Option C: `Executors.newVirtualThreadPerTaskExecutor()` as a Spring bean, used with `CompletableFuture.supplyAsync`.

### Rationale

It is the natural tool for I/O-bound work in Java 21, needs no tuning, and is a good point to explain in an interview. The futures are joined in the list order, so the result keeps the PokeAPI order.

### Consequences

- If one call fails, the whole page fails with `502` (simple and predictable; partial pages would confuse the client).
- The executor is injected in the use case, so tests can use the same executor or a direct one.

---

## TDR-003 — Redis cache configuration

### Context / Problem

We use the Spring cache abstraction (`@Cacheable`) with Redis. We have to choose how values are serialized, how long they live, and what happens when Redis is down.

### Options considered

#### Option A — Default configuration (JDK serialization, no TTL)
- No code.
- Domain records must implement `Serializable`; values are not readable in Redis; entries never expire; a Redis error breaks the request.

#### Option B — `RedisCacheManager` with JSON values, TTL and a `CacheErrorHandler`
- Values stored as JSON, readable with `redis-cli`. One TTL from configuration. Errors are logged and the call goes to PokeAPI.
- A small configuration class.

### Decision

Option B. Each cache uses a JSON serializer typed to its value class, so no type information is stored inside the JSON.

### Rationale

Readable values help debugging and the demo. The TTL avoids old data forever. The error handler makes Redis an optimization, not a single point of failure (AC-11).

### Consequences

- If a cached class changes its fields, old entries may fail to read. The error handler treats this as a cache miss, and the TTL removes them.
- Keys use a prefix (`poke-tech-test::`) so they do not mix with other data in a shared Redis.

---

## TDR-004 — Error response format

### Context / Problem

All endpoints need the same error format, with correct HTTP status codes and no internal details.

### Options considered

#### Option A — Custom error class (`ErrorResponse` with `timestamp`, `message`, ...)
- Full control over the fields.
- A format we invent and must document; Spring's own errors (for example, 405 or 415) still use the default format unless we map all of them.

#### Option B — RFC 9457 Problem Details (`ProblemDetail`)
- Standard format, supported natively by Spring MVC through `ResponseEntityExceptionHandler`.
- All Spring MVC errors already follow it. Custom fields (like `errors`) can be added as properties.

### Decision

Option B, with one `@RestControllerAdvice` that extends `ResponseEntityExceptionHandler`.

### Rationale

It is an industry standard, needs little code, and gives the same format for our errors and for framework errors.

### Consequences

- Validation errors add an `errors` list (`field`, `message`).
- `500` responses use a generic `detail`; the real cause is only in the log.

---

## TDR-005 — API documentation with springdoc-openapi

### Context / Problem

The interview includes a demo of the API, and the global coding rules ask to keep OpenAPI annotations up to date on controllers.

### Options considered

#### Option A — No OpenAPI; document endpoints only in Markdown
- No dependency.
- No interactive way to try the API; the docs can drift from the code.

#### Option B — springdoc-openapi with Swagger UI
- OpenAPI generated from the code; Swagger UI at `/swagger-ui.html` to try the endpoints (and JWT later).
- One dependency (version 3.1.1, built for Spring Boot 4.1).

### Decision

Option B.

### Rationale

It makes the demo and the review easier, and the contract always matches the code.

### Consequences

- Controllers use `@Tag`, `@Operation` and `@ApiResponse` only where they add information.
- Swagger UI is a public route.
