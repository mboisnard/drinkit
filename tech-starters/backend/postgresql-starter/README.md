# PostgreSQL Starter

A [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/tech-starters.md)
that gives the application a jOOQ `DSLContext` and a connection pool on PostgreSQL. It refuses a `@JooqRepository`
call outside a transaction, a statement that updates or deletes without a `WHERE`, and a database other than
PostgreSQL. It also bounds each statement in time, logs the slow queries and turns a lost update into
Spring's `OptimisticLockingFailureException`.

## What is it for?

The adapters of the
[hexagonal architecture](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/hexagonal-architecture.md)
read and write their tables with jOOQ, inside the transaction of the use case that calls them. The tables and their
changes belong to the
[updater](https://github.com/mboisnard/drinkit/blob/master/deployment/updater/README.md), which applies the Liquibase
changelogs before the application starts.

## Configuration

| Property | Default | Environment variable | Required in production |
|---|---|---|---|
| `spring.datasource.url` | None | `SPRING_DATASOURCE_URL` | Yes |
| `spring.datasource.username`, `spring.datasource.password` | None | `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | Yes |
| `starters.postgresql.slow-query-threshold` | `500ms` | `STARTERS_POSTGRESQL_SLOWQUERYTHRESHOLD` | No |
| `starters.postgresql.enabled` | `true` | `STARTERS_POSTGRESQL_ENABLED` | No |

The starter sets these defaults, which a property of the application or an environment variable overrides:

- `spring.datasource.hikari.auto-commit=false`: only the transaction of the use case commits.
- `spring.datasource.hikari.data-source-properties.options=-c statement_timeout=30s -c lock_timeout=5s
  -c idle_in_transaction_session_timeout=60s`. Setting `options` replaces all three.
- `spring.datasource.hikari.data-source-properties.ApplicationName` and `spring.datasource.hikari.pool-name` take
  `spring.application.name`, so `pg_stat_activity` tells the applications sharing a database apart. Without it,
  the driver and Hikari keep their own names.
- `spring.jooq.sql-dialect=postgres`: jOOQ never guesses the dialect, a guess that falls back to a generic one on failure.
- `logging.level.org.jooq.Constants=off`, which hides the jOOQ logo and its tips.

The application refuses to start on another database, since the SQL of the adapters is PostgreSQL's. A statement that
updates or deletes every row on purpose says so with `.where(trueCondition())`. A slow query is logged at `WARN` with
its bind values left as `?`, so that no personal data reaches the logs.

A `DefaultConfigurationCustomizer` bean named `postgresqlJooqCustomizer` replaces the starter's: the dialect, the
optimistic locking and the `WHERE` guards. A customizer with another name adds to them.

## Usage

The [`jooq-codegen-convention`](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/build-and-data/build-tool.md)
plugin adds the starter and its test fixtures to a module that generates jOOQ classes. A repository is a
`@JooqRepository`, a Spring repository whose methods require an open transaction: with auto-commit off, a call
without one would write nothing, so it fails.

```kotlin
@JooqRepository
internal class JooqCellars(private val dslContext: DSLContext) : Cellars {

    override fun findById(cellarId: CellarId): Cellar? = dslContext.selectFrom(CELLAR)
        .where(CELLAR.ID.eq(cellarId.value))
        .fetchOne { it.toCellar() }
}
```

`JSONBToJacksonConverter(CityLocation::class.java, jsonMapper)` maps a JSONB column to a Kotlin type with the
application's `JsonMapper`. A read whose size the repository cannot know returns a `Sequence` from `fetchSequence`,
an extension of a jOOQ `Select` that streams the rows from a cursor, a thousand at a time. The cursor lives as long as
the transaction, so the caller reads the `Sequence` inside it: a use case that returns one is
`@Transactional(propagation = MANDATORY)`, and the controller that maps it opens the transaction, as the architecture
tests check.

```kotlin
@Transactional(readOnly = true)
override fun findCellars() = ResponseEntity.ok(CellarsResponse(findCellars.byOwnerId(owner).map { it.toResponse() }.toList()))
```

`@JooqIntegrationTest(schemas = [DrinkitApplication::class])` gives the test a `DSLContext` on a database of its
own, in a PostgreSQL container, with the tables of the generated schema classes it names, such as
`DrinkitApplication` for `drinkit_application`. Each test is rolled back. The
[Testcontainers](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/writing-code/testing/testcontainers.md)
page says how the container is shared.

## Optimistic locking

A table whose version column the `jooq` block of the build names in `recordVersionFields` gets jOOQ's optimistic
locking on `store()`, `update()` and `delete()`. When another transaction changed the row since it was read, the
repository throws an `OptimisticLockingFailureException`. A `dsl.update(...)` skips the check: a versioned table is
written through its record.
