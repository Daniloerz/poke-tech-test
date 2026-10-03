# Bootstrap — Technical Decision Records

## TDR-001 — Java and Spring Boot versions

### Context / Problem

The project instructions set Java 17 as the baseline, but the machine only had JDK 19, which is not LTS and no longer gets updates. We need a supported JDK and a Spring Boot version that is still maintained.

### Options considered

#### Option A — Java 17 + Spring Boot 3.5
- Oldest LTS still supported; the most widely known stack.
- Spring Boot 3.5 open-source support has already ended, so new projects should not start on it.

#### Option B — Java 21 + Spring Boot 4.1
- Java 21 is LTS, has an official `eclipse-temurin:21` image, and offers virtual threads, records and pattern matching.
- Spring Boot 4.1 is the current supported line. It uses modular starters (`spring-boot-starter-webmvc`, `spring-boot-starter-liquibase`, ...).
- Less material online than for 3.x.

### Decision

Java 21 (portable local JDK, no global change) + Spring Boot 4.1.1.

### Rationale

Both are supported versions, they are the same in local and Docker, and virtual threads are useful for the parallel PokeAPI calls in US01.

### Consequences

- Commands are run with `JAVA_HOME` set per command, because the global JDK is still 19.
- Boot 4 module names differ from 3.x (for example, Liquibase needs `spring-boot-starter-liquibase`, not only `liquibase-core`).

---

## TDR-002 — Environment configuration

### Context / Problem

The backend runs in two ways: inside Docker Compose and from the IDE against the Compose services. Secrets must not be in the repository.

### Options considered

#### Option A — Backend reads a `.env` file (dotenv library or `spring.config.import`)
- One file for everything.
- Adds a dependency or a file-path coupling, and host names differ between the two run modes (`postgres:5432` vs `localhost:5433`), so one file does not fit both.

#### Option B — Environment variables with placeholders in `application.yml`
- `.env` is read only by Docker Compose, which passes the values as environment variables.
- Non-sensitive values have local defaults. Secrets have no default, so the app fails at startup if they are missing.

### Decision

Option B.

### Rationale

This is the standard Spring / 12-factor approach. It needs no extra library and keeps secrets out of Git.

### Consequences

- To run from the IDE, `DATABASE_PASSWORD` (and later `JWT_SECRET`) must be set in the run configuration.
- `.env.example` documents every variable; `.env` is ignored by Git.

---

## TDR-003 — Database migrations with Liquibase

### Context / Problem

The schema and the seed data must be created automatically when the backend starts, in Compose and in local runs.

### Options considered

#### Option A — Liquibase with SQL changelogs
- Versioned and runs on startup.
- The developer already has experience with it.

#### Option B — Flyway
- Also versioned and slightly simpler (`V1__x.sql`).
- No real advantage here over the tool the developer knows best.

#### Option C — Postgres init scripts (`docker-entrypoint-initdb.d`)
- No extra dependency.
- Only runs on an empty volume and does not work when the backend runs outside Compose.

### Decision

Liquibase, with a YAML master changelog that includes SQL changesets in order. File names do not use a ticket ID (personal project).

### Rationale

It gives reproducible migrations in every environment, and the developer can explain it well.

### Consequences

- `hibernate.ddl-auto=validate`: Hibernate never changes the schema; it only checks that the entities match it.
- `docs/bd/ddl.sql` and `dml.sql` are a consolidated copy and must stay in sync with the changelogs.

---

## TDR-004 — Load Mockito as a Java agent in the tests

### Context / Problem

Mockito creates mocks with an agent. By default it attaches the agent to the running JVM at runtime ("self-attach"). Since Java 21 the JDK warns about this, and future versions will block it. During the final validation, the tests run in a clean `eclipse-temurin:21-jdk` container failed: 116 of 174 tests with "Could not self-attach to current VM". On the developer machine they passed.

### Options considered

#### Option A — Keep self-attach
- No configuration.
- Depends on the environment (it failed in a clean container) and stops working in future JDKs.

#### Option B — Load Mockito with `-javaagent` in Surefire
- `maven-dependency-plugin:properties` exposes the Mockito jar path; Surefire starts the test JVM with `-javaagent:"<mockito-core.jar>"`. This is the setup recommended by the Mockito documentation.
- A few lines in `pom.xml`. The path is quoted because the Maven repository can be in a folder with spaces.

### Decision

Option B.

### Rationale

The tests must give the same result on every machine and in CI.

### Consequences

- No "self-attaching" warning in the test output.
- Running a test from an IDE that does not use Surefire's `argLine` may still use self-attach (it works on the developer machine).
