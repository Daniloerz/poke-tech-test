# User Authentication — Implementation Plan

## Endpoints

| Method | Path | Access | Request | Response |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Public | `{ "username": "brock", "password": "onix12345" }` | `201` `{ "id": 3, "username": "brock", "createdAt": "2026-10-03T20:00:00Z" }` + `Location: /api/v1/auth/me` |
| `POST` | `/api/v1/auth/login` | Public | `{ "username": "ash", "password": "pikachu123" }` | `200` `{ "accessToken": "eyJ...", "tokenType": "Bearer", "expiresIn": 3600 }` |
| `GET` | `/api/v1/auth/me` | Token | — | `200` `{ "id": 1, "username": "ash", "createdAt": "..." }` |

Errors (Problem Details): `400` (validation, with `errors`; malformed JSON), `401` (bad credentials or missing/invalid token), `409` (username taken).

## Package structure (US01 ADR-001 applies)

```text
domain/model/                       User
application/
├── port/out/                       UserRepository, PasswordHasher, TokenIssuer, IssuedToken
├── exception/                      UsernameAlreadyExistsException, InvalidCredentialsException
└── usecase/                        RegisterUserUseCase, LoginUseCase, GetCurrentUserUseCase
infrastructure/
├── persistence/                    UserEntity, UserJpaRepository (Spring Data), UserRepositoryAdapter, UserEntityMapper
├── security/                       SecurityConfig, BcryptPasswordHasher, JwtTokenIssuer,
│                                   ProblemDetailAuthenticationEntryPoint, ProblemDetailAccessDeniedHandler
└── config/                         JwtProperties
interfaces/rest/                    AuthController, dto/ (RegisterRequest, LoginRequest, UserResponse, TokenResponse)
```

Why ports for hashing and tokens: the use cases stay free of Spring Security and are tested with simple mocks; the adapters are thin wrappers around `PasswordEncoder` and `JwtEncoder` (ADR-002).

## Flows

```text
register:  AuthController → RegisterUserUseCase
             normalize username → UserRepository.findByUsername → present? 409
             → PasswordHasher.hash → UserRepository.save → User → 201

login:     AuthController → LoginUseCase
             normalize username → UserRepository.findByUsername
             → user found ? PasswordHasher.matches(raw, hash) : PasswordHasher.matches(raw, DUMMY_HASH) → false? 401
             → TokenIssuer.issue(user) → 200

me:        Spring Security validates the JWT (resource server) → AuthController reads `uid` from the Jwt
             → GetCurrentUserUseCase → UserRepository.findById → 200 (404 is impossible in practice; mapped to 401)
```

The dummy hash comparison makes the response time similar for unknown users and wrong passwords (AC-09).

## Domain

- `User` (record): `id`, `username`, `passwordHash`, `createdAt` (`Instant`). The REST layer never exposes `passwordHash`.
- Username normalization (`trim` + lower case) is a domain rule: static method `User.normalizeUsername(String)`.

## Application

- `UserRepository` (out port): `Optional<User> findByUsername(String)`, `Optional<User> findById(long)`, `User save(User)`.
- `PasswordHasher` (out port): `String hash(String raw)`, `boolean matches(String raw, String hash)`.
- `TokenIssuer` (out port): `IssuedToken issue(User user)`; `IssuedToken(String value, long expiresInSeconds)`.
- `RegisterUserUseCase` (`@Transactional`): the unique constraint in the database is the final guard; a `DataIntegrityViolationException` on save (two registrations at the same time) is also mapped to `UsernameAlreadyExistsException` in the adapter.
- `LoginUseCase`, `GetCurrentUserUseCase`.

## Infrastructure

### Persistence (BDDR in `docs/bd/bddr.md`)

- Liquibase changesets (SQL format) in `db/changelog/changes/`:
  - `20261003-01-create-app-user.sql`
  - `20261003-02-seed-app-user.sql`
- `UserEntity` (`@Entity`, table `app_user`), `UserJpaRepository extends JpaRepository<UserEntity, Long>` with `findByUsername`.
- `UserRepositoryAdapter` implements `UserRepository`; `UserEntityMapper` (MapStruct) entity ↔ domain.
- `createdAt` is set by the database default (`now()`); the entity reads it back after insert.

### Security (ADR-001, TDR-001..003)

- `SecurityConfig`:
  - `SecurityFilterChain`: stateless sessions, CSRF disabled (no cookies), HTTP Basic and form login disabled.
  - Public routes from AC-13; everything else `authenticated()`.
  - `oauth2ResourceServer(jwt)` with a `NimbusJwtDecoder` for HS256 using the shared secret.
  - `AuthenticationEntryPoint` and `AccessDeniedHandler` that write Problem Details (`401` / `403`).
  - `PasswordEncoder` bean: `BCryptPasswordEncoder` (strength 10).
- `JwtTokenIssuer`: `NimbusJwtEncoder` with the same secret; claims `sub`, `uid`, `iat`, `exp`, `iss`.
- `JwtProperties` (`@ConfigurationProperties("security.jwt")`, validated): `secret` (min 32 bytes), `expiration` (default `1h`), `issuer`.

## Interfaces (REST)

- `AuthController`: `@Valid` request bodies (`@NotBlank`, `@Size`, `@Pattern`), OpenAPI annotations, `bearerAuth` security scheme declared in an `OpenApiConfig` so Swagger UI has an "Authorize" button.
- `GlobalExceptionHandler` gains:
  - `MethodArgumentNotValidException` → `400` with `errors` (body validation; until now only parameters were validated).
  - `HttpMessageNotReadableException` → `400` "Malformed request body".
  - `UsernameAlreadyExistsException` → `409`.
  - `InvalidCredentialsException` → `401` "Invalid username or password."

## Configuration

| Property | Env variable | Default |
|---|---|---|
| `security.jwt.secret` | `JWT_SECRET` | none (required, at least 32 bytes) |
| `security.jwt.expiration` | `JWT_EXPIRATION` | `1h` |
| `security.jwt.issuer` | — | `poke-tech-test` |

`docker-compose.yml` passes `JWT_SECRET` to the backend; `.env.example` documents it with a placeholder.

## New dependencies

| Dependency | Reason |
|---|---|
| `spring-boot-starter-security` | Security filter chain, `PasswordEncoder`. |
| `spring-boot-starter-security-oauth2-resource-server` | JWT validation and `NimbusJwtEncoder` (Nimbus JOSE), no custom filter (TDR-001). |
| `spring-boot-starter-security-test` (test) | `jwt()` request post-processor for controller tests. |

## Tests (unit only, decision 12)

| AC | Tests |
|---|---|
| AC-01, AC-02, AC-06, AC-07 | `RegisterUserUseCaseTest` (mocks of the ports) |
| AC-02 | `UserTest` (normalization) |
| AC-03, AC-04, AC-05, AC-06 | `AuthControllerTest` (`@WebMvcTest` + `SecurityConfig`, use cases mocked) |
| AC-08, AC-09 | `LoginUseCaseTest` (dummy hash is compared when the user does not exist), `AuthControllerTest` |
| AC-10 | `AuthControllerTest` |
| AC-11 | `JwtTokenIssuerTest` (real encoder and decoder, no Spring context: claims, expiry, signature) |
| AC-12 | `AuthControllerTest` with `jwt()` |
| AC-13, AC-14, AC-15 | `SecurityRulesTest` (`@WebMvcTest` + `SecurityConfig`: public routes without token, protected route without token / with invalid token → `401` Problem Details + `WWW-Authenticate`) |
| AC-16 | Manual check with Docker Compose (login with the demo users) |
| — | `BcryptPasswordHasherTest` (hash is not the raw value, matches) |

The JPA adapter and the Liquibase changesets are checked with Docker Compose (database tests are backlog item 1).

Existing `@WebMvcTest` tests (`PokemonControllerTest`) import `SecurityConfig`, so they run with the real security rules.

## Security

- Generic login error; dummy hash comparison against user enumeration by timing.
- BCrypt (salted, slow). Passwords and tokens never logged.
- Secret only from environment, validated at startup. HS256 with a 256-bit minimum key.
- Stateless: no CSRF risk from cookies.

## Documentation to update

- `docs/bd/` (new): `erd.mmd`, `ddl.sql`, `dml.sql`, `bddr.md`.
- README: auth endpoints, demo users, `JWT_SECRET`, how to call a protected route.
