# Poke Tech Test

REST API built with Java and Spring Boot that integrates [PokeAPI](https://pokeapi.co/docs/v2). It lets users browse Pokemon, see their details, copy them into a local PostgreSQL database and edit that local copy with their own fields.

The exercise statement is in [`java_technical_interview_exercise.md`](java_technical_interview_exercise.md).

> **Status:** backend in progress. Done: US01 (Pokemon list). The frontend will start once the backend is complete.

## Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Maven (wrapper), Lombok, MapStruct |
| Database | PostgreSQL 17, Liquibase migrations |
| Cache | Redis 7.4 |
| Tests | JUnit 5, Mockito |
| Infrastructure | Docker, Docker Compose |

## Prerequisites

- Docker with Docker Compose v2.
- To run the backend or the tests outside Docker: JDK 21. Maven is not needed (use `./mvnw`).

## Run everything with Docker Compose

```bash
cp .env.example .env
```

Edit `.env` and set `POSTGRES_PASSWORD`. Then:

```bash
docker compose up --build
```

Health check: `http://localhost:8080/actuator/health`

## Run services separately

Start only the infrastructure:

```bash
docker compose up -d postgres redis
```

Run the backend from the IDE or the terminal. The secrets have no default, so set them first (use the same password as in `.env`):

```bash
cd backend
DATABASE_PASSWORD=<your-password> ./mvnw spring-boot:run
```

## API

Interactive documentation (Swagger UI): `http://localhost:8080/swagger-ui.html`

| Method | Path | Access | Story | Description |
|---|---|---|---|---|
| `GET` | `/api/v1/pokemon?page=0&size=20` | Public | US01 | Paginated list from PokeAPI (sprite, types, weight in kg, moves). `size` from 1 to 50. |

Errors use the RFC 9457 Problem Details format (`application/problem+json`): `400` for invalid input, `502` when PokeAPI is not available, `500` for unexpected errors.

PokeAPI responses are cached in Redis for 24 hours. If Redis is down, the API still works without the cache.

## Environment variables

All variables are documented in [`.env.example`](.env.example). `.env` is read only by Docker Compose; the backend reads plain environment variables.

| Variable | Used by | Default |
|---|---|---|
| `POSTGRES_DB` / `POSTGRES_USER` | Compose | `pokemon` / `pokemon` |
| `POSTGRES_PASSWORD` | Compose | none (required) |
| `POSTGRES_HOST_PORT` | Compose | `5433` |
| `REDIS_HOST_PORT` | Compose | `6380` |
| `BACKEND_HOST_PORT` | Compose | `8080` |
| `DATABASE_URL` | Backend | `jdbc:postgresql://localhost:5433/pokemon` |
| `DATABASE_USER` | Backend | `pokemon` |
| `DATABASE_PASSWORD` | Backend | none (required) |
| `SERVER_PORT` | Backend | `8080` |
| `REDIS_HOST` / `REDIS_PORT` | Backend | `localhost` / `6380` |
| `POKEAPI_BASE_URL` | Backend | `https://pokeapi.co/api/v2` |
| `POKEAPI_CONNECT_TIMEOUT` / `POKEAPI_READ_TIMEOUT` | Backend | `3s` / `5s` |
| `POKEAPI_CACHE_TTL` | Backend | `24h` |

The host ports avoid the usual `5432` and `6379`, which are often used by other local containers.

## Tests

Unit tests use JUnit 5 and Mockito. They do not need Docker or a database.

```bash
cd backend
./mvnw test
```

## Documentation map

| Path | Content |
|---|---|
| `backend/docs/features/<feature>/` | Context, implementation plan, ADRs and TDRs of each feature |
| `backend/docs/bd/` | ERD, DDL, DML and database decisions (BDDR) |
| `CLAUDE.md` | Instructions and approved decisions for the AI development agent |

## Main assumptions

- US01 and US02 read from PokeAPI (with a Redis cache). US03 and US04 work on the local database.
- In US01, "category" means the Pokemon `types` and "skills" means its `moves`.
- Write operations on local data require an authenticated user (JWT). There are no roles for now.
