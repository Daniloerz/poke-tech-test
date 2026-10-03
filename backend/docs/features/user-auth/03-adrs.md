# User Authentication — Architecture Decision Records

## ADR-001 — Authentication mechanism

### Context / Problem

The API needs registration, login and protected routes. The client will be a single-page React app. The backend must stay simple and easy to run with Docker Compose.

### Options considered

#### Option A — Server sessions with a cookie
- Classic Spring Security login; the server keeps the session.
- State on the server (or in Redis); needs CSRF protection and cookie/CORS settings for a separate frontend.

#### Option B — HTTP Basic on every request
- No token at all.
- The client must keep and send the password on every request; no expiry; poor practice for a SPA.

#### Option C — Stateless JWT (Bearer token)
- Login returns a signed token with an expiry; the client sends it in the `Authorization` header.
- No server state; no CSRF risk; standard for SPAs and easy to try in Swagger UI.
- A token cannot be revoked before it expires (acceptable here; short lifetime).

### Decision

Option C, signed with HS256 and a shared secret, validated by Spring Security's OAuth2 resource server support.

### Rationale

It fits a REST API consumed by a SPA, keeps the backend stateless, and is the mechanism interviewers expect to see explained.

### Consequences

- Logout is client-side (delete the token). Revocation and refresh tokens are out of scope.
- The secret is the most sensitive configuration value: environment only, validated at startup.
- One service signs and validates, so a symmetric key (HS256) is enough; an asymmetric key (RS256) would only be needed if other services validated the tokens.

---

## ADR-002 — Ports for password hashing and token issuing

### Context / Problem

The register and login use cases need to hash passwords and create tokens. Both are provided by Spring Security (`PasswordEncoder`, `JwtEncoder`). We must decide whether the application layer uses these framework types directly.

### Options considered

#### Option A — Use `PasswordEncoder` and `JwtEncoder` directly in the use cases
- Fewer types.
- The application layer depends on Spring Security, and JWT details (claims, expiry) leak into the use case.

#### Option B — Out ports `PasswordHasher` and `TokenIssuer`, implemented in `infrastructure/security`
- The use cases only say "hash this" and "issue a token for this user"; tests mock two small interfaces.
- Two small adapter classes.

### Decision

Option B.

### Rationale

It follows the same rule as PokeAPI and the database (US01 ADR-001): infrastructure details live behind out ports. The adapters are thin, so the extra cost is small, and the login logic (including the anti-enumeration rule) is tested without Spring.

### Consequences

- Changing the hashing algorithm or the token format only touches `infrastructure/security`.
- Token validation is not behind a port: it happens in the Spring Security filter chain, before any use case runs.
