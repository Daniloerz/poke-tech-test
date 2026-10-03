# User Authentication

## Feature

User registration, login with JWT, and public versus protected routes.

## Goal

Give the API a user collection and a stateless authentication mechanism, so that the write operations on local data (US03, US04) can be protected while the PokeAPI catalog stays public.

## Functional context

The exercise asks for:

- a relational store with a primary entity and a **secondary collection for user management**;
- an **auxiliary API for user registration, authentication, and the management of protected versus public routes**;
- seeded data or **mock credentials** for the demo.

This feature creates the first database table (`app_user`), the first Liquibase changesets and the security configuration. US03 and US04 will reuse it: their routes are protected by default.

## Actors

- **Visitor:** not authenticated. Can browse the catalog, register and log in.
- **Authenticated user:** has a valid JWT. Can use protected routes. There are no roles for now (Phase 0 decision 6; roles are in the backlog).

## User stories

> As a visitor, I want to create an account, so that I can log in.

> As a registered user, I want to log in with my username and password, so that I get a token to use the protected operations.

## Acceptance criteria

### Registration — `POST /api/v1/auth/register`

| ID | Criterion |
|---|---|
| AC-01 | A valid `{username, password}` creates the user and returns `201` with `{id, username, createdAt}` and a `Location` header. The password is never returned. |
| AC-02 | The username is trimmed and stored in lower case. `Ash` and `ash` are the same user. |
| AC-03 | The username must have 3 to 30 characters: letters, digits, `.`, `_` or `-`. Otherwise `400` with `errors`. |
| AC-04 | The password must have at least 8 characters and at most 72 bytes in UTF-8 (the BCrypt limit; `ñ` counts as 2 bytes). Otherwise `400` with `errors`. |
| AC-05 | A missing field or a malformed JSON body returns `400`. |
| AC-06 | A username that already exists returns `409`. |
| AC-07 | The password is stored as a BCrypt hash, never in plain text. |

### Login — `POST /api/v1/auth/login`

| ID | Criterion |
|---|---|
| AC-08 | Valid credentials return `200` with `{accessToken, tokenType: "Bearer", expiresIn}` (seconds). The username is matched case-insensitively. |
| AC-09 | A wrong password or an unknown username returns `401` with the same generic message, so a client cannot learn which usernames exist. |
| AC-10 | A missing field or a malformed JSON body returns `400`. |
| AC-11 | The token is a signed JWT (HS256) with `sub` = username, `uid` = user id, `iat` and `exp`. Lifetime is configurable (default 1 hour). |

### Current user — `GET /api/v1/auth/me`

| ID | Criterion |
|---|---|
| AC-12 | With a valid token, returns `200` with `{id, username, createdAt}` of the token owner. |

### Route protection

| ID | Criterion |
|---|---|
| AC-13 | Public routes: `GET /api/v1/pokemon/**`, `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, Swagger UI and OpenAPI docs, `GET /actuator/health`. On these routes an `Authorization` header is ignored, so a stale or invalid token never turns a public route into a `401`. |
| AC-14 | Any other route requires a valid token. Without a token, or with an invalid, expired or wrongly signed token, the API returns `401` in the common error format with a `WWW-Authenticate: Bearer` header. |
| AC-15 | The API is stateless: no session and no cookies. |

### Demo data

| ID | Criterion |
|---|---|
| AC-16 | The database starts with two demo users documented in the README: `ash` / `pikachu123` and `misty` / `starmie123`. |

## Business rules

- Usernames are unique and case-insensitive.
- Passwords are only stored hashed. 72 bytes (UTF-8) is the BCrypt input limit.
- Login error messages never reveal whether the username exists.

## Main flows

- Register → `201`. Login → token. Call a protected route with `Authorization: Bearer <token>`.

## Alternative flows

- Login with a different case in the username (`ASH`) → works.

## Error flows

- Invalid input → `400`. Username taken → `409`. Bad credentials → `401`. Missing or invalid token on a protected route → `401`.

## Dependencies

- None. US03 and US04 depend on this feature.

## Explicit assumptions

- No roles or permissions yet: every authenticated user can use every protected route (backlog item 3).
- No refresh tokens, logout or token revocation: the token expires on its own. This is enough for the exercise and keeps the API stateless.
- No password complexity rules besides length (current NIST guidance prefers length over composition rules).
- No rate limiting or account lock on login (out of scope; listed as a risk).
- CORS is configured when the frontend is built.

## Non-functional requirements

- The JWT secret comes from the `JWT_SECRET` environment variable, with no default. The app fails at startup if it is missing or shorter than 32 bytes (256 bits, required by HS256).
- Passwords and tokens are never written to the logs.
