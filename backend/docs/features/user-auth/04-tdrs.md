# User Authentication — Technical Decision Records

## TDR-001 — JWT library and validation

### Context / Problem

We need to create and validate JWTs. Validation must run on every protected request.

### Options considered

#### Option A — jjwt + a custom `OncePerRequestFilter`
- Very common in tutorials.
- We write and maintain the filter: header parsing, error handling, setting the security context. Easy to get subtle details wrong.

#### Option B — Spring Security OAuth2 resource server (Nimbus JOSE)
- `oauth2ResourceServer().jwt()` validates signature, expiry and format, and returns `401` with `WWW-Authenticate`. `NimbusJwtEncoder` creates the tokens.
- Official Spring support; no custom filter. Slightly less known for "own login" scenarios.

### Decision

Option B, with a `NimbusJwtDecoder` and `NimbusJwtEncoder` built from the same HS256 secret.

### Rationale

Less custom security code means fewer bugs. It is the approach recommended by the Spring Security team for JWT bearer tokens.

### Consequences

- The authenticated principal in controllers is a `Jwt`; the user id is read from the `uid` claim.
- The issuer (`iss`) is validated together with the expiry.

---

## TDR-002 — Password hashing

### Context / Problem

Passwords must be stored so that a database leak does not reveal them.

### Options considered

#### Option A — BCrypt
- Salted and slow by design; default in Spring Security; widely known.
- Input limited to 72 bytes.

#### Option B — Argon2
- Modern winner of the Password Hashing Competition, memory-hard.
- Needs an extra library (Bouncy Castle) and tuning; harder to explain quickly.

### Decision

BCrypt with strength 10, through `BCryptPasswordEncoder`.

### Rationale

Secure enough for this project, no extra dependency, and the seed hashes can be generated with the same library.

### Consequences

- The password length limit is 72 characters (AC-04), so no input is silently truncated.

---

## TDR-003 — Error format for 401 and 403

### Context / Problem

Security errors happen in the filter chain, before the controller, so `@RestControllerAdvice` does not handle them. By default Spring Security answers `401` with an empty body.

### Options considered

#### Option A — Keep the default responses
- No code.
- Different error format from the rest of the API; clients need special cases.

#### Option B — Custom `AuthenticationEntryPoint` and `AccessDeniedHandler` that write Problem Details
- Same format as every other error; keeps the `WWW-Authenticate: Bearer` header.
- Two small classes.

### Decision

Option B.

### Rationale

One error format for the whole API (US01 TDR-004).

### Consequences

- The entry point delegates the header to Spring's `BearerTokenAuthenticationEntryPoint` and only writes the body.
- The `detail` is generic ("Authentication is required or the token is invalid."); the exact reason is only logged at `DEBUG`.
