# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DrinkIt is a wine & spirit cellar management application. Stack: **Kotlin + Spring Boot 3** backend, **Nuxt 3 + Vue 3 + PrimeVue** frontend, **PostgreSQL** (via JOOQ), and **Meilisearch** for search.

## Build & Run Commands

### Backend
```bash
./gradlew :drinkit-backend:build          # Full build
./gradlew :drinkit-backend:bootRun        # Run app (auto-starts Docker Compose for dev dependencies)
./gradlew test                            # Run all tests
./gradlew :drinkit-domain:test            # Run tests for a specific module
./gradlew :drinkit-infra:test             # Run infrastructure (integration) tests
./gradlew jooqCodegen                     # Regenerate JOOQ classes (requires PostgreSQL on localhost:5432)
```

`bootRun` activates the `dev` Spring profile, which uses Spring Boot Docker Compose to start the local `deployment/local/compose.yml` (PostgreSQL on 5432, Meilisearch on 7700).

**Endpoints (local):**
- API: `http://localhost:8080/drinkit/api/cellars`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- Actuator: `http://localhost:8080/actuator`

### Frontend
```bash
cd drinkit/drinkit-frontend
npm i
npm run generate:client-api   # Generates TypeScript client from OpenAPI spec (requires Java in PATH)
npm run dev                   # http://localhost:3000/cellars
```

## Architecture

### Gradle Multi-Module Structure

The project uses dynamically included Gradle subprojects. `settings.gradle.kts` auto-discovers modules with a `build.gradle.kts`.

**Application modules** (under `drinkit/`):
- `drinkit-api-contract` — OpenAPI YAML definitions only (consumed by `drinkit-backend` for code generation)
- `drinkit-domain` — domain layer: use cases, core entities, SPI interfaces; no framework dependency except Spring `@Service`/`@Transactional`
- `drinkit-infra` — infrastructure: JOOQ repositories implementing domain SPI interfaces, external HTTP clients (Feign)
- `drinkit-backend` — Spring Boot application assembly; implements generated OpenAPI delegate interfaces to wire HTTP → use cases
- `drinkit-frontend` — Nuxt 3 app consuming a TypeScript client generated from the same OpenAPI spec

**Technical starters** (under `tech-starters/backend/`): reusable backend libraries (messaging, event-sourcing, security, search-engine, postgresql, mail, ocr, sse, webclient, etc.).

**Build conventions** (under `buildSrc/src/main/kotlin/`):
- `com.drinkit.common-convention` — applied to every module: Kotlin JVM 25 toolchain, Java 25 target, Spring plugins, platform BOM enforcement
- `com.drinkit.api-convention` — Spring Boot application (adds `spring-boot-starter-web`, monitoring, git properties)
- `com.drinkit.library-convention` — Spring library (adds `spring-context`, `spring-tx`, event-sourcing, kotlin-starter)
- `com.drinkit.contract-first` — OpenAPI code-generation via `openapi-generator` (`kotlin-spring` generator, delegate pattern)
- `com.drinkit.jooq-codegen-convention` — JOOQ Kotlin code generation; only runs when `jooqCodegen` is explicitly requested
- `com.drinkit.test-fixtures-convention` — enables `java-test-fixtures` source set

### Domain Architecture Patterns

The codebase applies **Hexagonal Architecture**, **CQRS**, and **Functional Core / Imperative Shell**:

- **SPI interfaces** (e.g., `Cellars`, `Users`) are defined in `drinkit-domain` under `*.spi` packages. Infrastructure implementations (e.g., `JooqCellars`) live in `drinkit-infra` and are injected at runtime.
- **Use cases** are single-responsibility `@Service` classes with an `invoke` operator (e.g., `CreateCellar`, `CreateNewUser`). Each takes a `*Command` data class.
- **Functional Core / Imperative Shell**: pure decision logic is extracted into internal `object` classes annotated `@FunctionalCore` (no I/O, fully testable). The `@ImperativeShell` service orchestrates I/O around them.
- **Event Sourcing** is used for the `User` aggregate. `UserEvent` subtypes are stored via `UserEvents` SPI; `User.from(history)` rebuilds state using `EventsReducer`.

### Documentation Annotations (KSP-generated docs)

`documentation-starter` provides marker annotations processed at compile time by a KSP processor to generate architecture documentation:
- `@CoreDomain` — marks a core domain entity
- `@Usecase` — marks an application use case
- `@Command` / `@Query` — marks CQRS command/query data classes
- `@FunctionalCore` / `@ImperativeShell` — marks FCIS boundaries
- `@Aggregate` / `@Projection` — marks event-sourcing roles

### Contract-First API Flow

1. API defined in YAML under `drinkit/drinkit-api-contract/contract/api-definition.yaml`
2. `drinkit-backend` depends on it via the `openApiInput` configuration and generates delegate interfaces during build
3. API controller classes (e.g., `CellarsApi`) implement those generated delegate interfaces
4. The same YAML drives `npm run generate:client-api` to produce the TypeScript frontend client

### JOOQ Code Generation

JOOQ classes are committed under `src/generated/jooq/kotlin` (not generated on every build). To regenerate after schema changes:
1. Start PostgreSQL: `docker compose -f deployment/local/compose.yml up postgresql`
2. Run migrations (via the app or Liquibase)
3. Run `./gradlew jooqCodegen` in the relevant module

### Testing Patterns

- **Domain tests** (no I/O): inject in-memory SPI implementations from `testFixtures` (e.g., `InMemoryCellars`, `InMemoryUsersRepository`). Fixtures can use `kotlin-faker` for random data and `ControlledClock` / `ControlledRandom` from `test-starter`.
- **Infrastructure tests**: annotate with `@JooqIntegrationTest(schemas = [DrinkitApplication::class])`. This JUnit extension spins up a Testcontainers PostgreSQL container, creates a dedicated database per test class using JOOQ DDL export, and rolls back after each test — no Liquibase needed.
- **Test contracts**: domain `testFixtures` expose abstract `*TestContract` classes (e.g., `CellarsTestContract`). Infrastructure tests extend these to verify the real implementation satisfies the domain contract.
- **Kotest** is used for assertions throughout.

### Local Infrastructure

`deployment/local/compose.yml` defines:
- `postgresql` — port 5432
- `meilisearch`— port 7700
