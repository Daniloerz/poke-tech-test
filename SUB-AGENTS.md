# Registro de subagentes

## Índice

| ID | Fecha | Nombre | Modelo | Feature | Estado de revisión |
|---|---|---|---|---|---|
| SA-001 | 2026-10-03 | Independent backend code review | Opus | Backend final validation (CLAUDE.md section 26) | Aprobada con ajustes |

---

## SA-001 — Independent backend code review

- **Fecha:** 2026-10-03
- **Modelo:** Opus
- **Feature / rama / worktree:** Backend final validation / `develop` / no worktree (read-only task, no files written)
- **Motivo de la delegación y del modelo elegido:** Section 26 asks for a final quality review. An independent reviewer that did not write the code finds problems the author no longer sees. It reads many files, which would fill the main context. Opus because it is a critical review where missing a real defect is expensive (section 22.3).

### Encargo

- **Objetivo:** Review the whole backend (`backend/src`, `backend/docs`, `docker-compose.yml`, `.env.example`, `README.md`) and report real defects: bugs, security problems, inconsistencies between code and documentation (ADR/TDR/BDDR, plans, README), dead code, duplication, over-engineering, configuration problems, and violations of the Java rules in the global CLAUDE.md that apply to this project.
- **Archivos permitidos:** read only, the whole repository. No writes, no git commands that change state, no Docker.
- **Criterios de aceptación:** each finding has file and line, severity (high / medium / low), a concrete failure scenario, and a suggested fix. No style nitpicks without impact.
- **Restricciones / decisiones vigentes a respetar:** decisions in CLAUDE.md section 0 and the approved ADR/TDR/BDDR (Java 21, Spring Boot 4.1, unit tests only, JWT HS256, text[] arrays, strict request bodies, frontend deferred). The reviewer must not reopen them; it can only point out where the code does not follow them.

### Resultado devuelto

- **Resumen:** Good overall quality: clean layers, no relevant violations of the Java rules, Liquibase changesets identical to `ddl.sql`/`dml.sql`/`erd.mmd`, consistent errors. No high-severity defect. 16 findings (3 medium, 13 low) and 3 proposals (P1-P3).
- **Archivos creados/modificados:** none (read-only).
- **Validaciones:** read-only greps for rule violations and stale doc terms — PASS; check in `spring-security-crypto-7.1.1.jar` of the BCrypt "more than 72 bytes" error — confirmed; visual diff changesets vs docs — PASS.

### Resumen de la investigación

- **Qué analizó o consultó:** all main and test sources, configuration, changesets, Dockerfile, Compose, README, acceptance criteria of every feature, relevant TDR/BDDR, CLAUDE.md section 0.
- **Alternativas consideradas y descartadas:** discarded as non-issues: Optional caching with null values, path traversal through `idOrName`, 409 vs rollback in register, sync race, double `findById` in delete, offset overflow, Dockerfile jar glob, secrets in logs, entity setters.
- **Cómo llegó a la solución:** reading every class against the acceptance criteria and the docs, plus targeted greps and one bytecode check (BCrypt limit).

### Decisiones del subagente

| # | Decisión | Tipo | Justificación del subagente |
|---|---|---|---|
| — | None. The subagent changed nothing and only proposed (P1 public routes with stale token, P2 demo seed per environment, P3 password byte limit). | — | — |

### Supuestos y dudas escaladas

- Assumed `BCrypt.checkpw` (login) does not enforce the 72-byte limit; not verified. The main agent added the byte limit to the login request too, so the answer is `400` either way.
- P1, P2, P3 escalated as proposals (see review below).

### Revisión del agente principal

- **Estado:** Aprobada con ajustes.
- **Coherencia con ADR/TDR/BDDR y decisiones previas:** no finding reopens an approved decision; findings #1, #2, #5, #8 are differences between code and acceptance criteria, #7 and #10 are stale docs.
- **Ajustes realizados o motivo de rechazo:** all 16 findings accepted and applied in `feature/backend-final-validation` (commits `b7312df`, `8b58b61`):
  - #1 / P3 option A: `@MaxUtf8Bytes(72)` on register and login passwords (400 instead of 500), test added.
  - #2 / P1 option B: `BearerTokenResolver` ignores the token on public routes (one shared `RequestMatcher`), tests added.
  - #3: `.env.example` JWT placeholder shorter than 32 characters (the app refuses to start until a real secret is set; verified). P2 option A: seed kept, note added to BDDR-006.
  - #4: users adapter only maps `uk_app_user_username` (shared `ConstraintViolations` helper), tests with a real Hibernate cause.
  - #5, #7, #8, #10: docs fixed. #6: concurrent delete during update → 404, test added. #9: unused field removed. #11: cause logged at DEBUG. #12: Postgres and Redis bound to 127.0.0.1 (access from Windows verified). #13: `LoginUseCase` without `Optional.get()`. #14: duplicated token tests merged. #15: tests added. #16: README updated.
  - Not proposed by the subagent but found during the same validation: Mockito self-attach failing in a clean container (116 errors) → Mockito loaded as a Java agent (bootstrap TDR-004).
- **Formalizado en:** `backend/docs/features/user-auth/01-context.md` (AC-04, AC-13), `user-auth/04-tdrs.md` (TDR-001, TDR-002), `bd/bddr.md` (BDDR-006), `us04-local-pokemon-management/01-context.md` (AC-04), `bootstrap/04-tdrs.md` (TDR-004).
- **Acciones de seguimiento:** demo seed per environment (Liquibase `context`) added to the backlog in `CLAUDE.md`.
