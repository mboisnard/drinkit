# 0007. jooq-kotlin maps nested MULTISET reads

- Status: accepted
- Date: 2026-11-02
- Issue: [#390](https://github.com/mboisnard/drinkit/issues/390)

An adapter that reads parents with their children, such as cellars with their bottles, does it in one query with
jOOQ's `MULTISET`. `postgresql-starter` brings `org.jooq:jooq-kotlin` so that the nested rows map to Kotlin
constructors in one call per level.

## Context

`postgresql-starter` is the [tech starter](../architecture/tech-starters.md) that gives every module its jOOQ
`DSLContext`. Its adapters read one table each today, and the coming features will read parents with their
children. A join returns the parent again on each child row, and an inner join loses the parents without children.
A query per parent is an N+1, which the diagnostics of the
[test extension](../writing-code/testing/testcontainers.md) log. `MULTISET` returns each parent once, its children
nested in its row, which PostgreSQL builds with `JSONB_AGG`. jOOQ 3.21 maps such a nested result with
`Records.mapping` wrapped in a `convertFrom` at each level.

## Decision

An adapter reads parents with their children through `MULTISET`, not through a join or a query per parent. The
starter declares `jooq-kotlin` as an `api` dependency, so every module that reads with jOOQ gets its
`mapping(::Constructor)` on nested fields. It takes the `jooq` version of the Gradle version catalog, since its
extensions are compiled against that jOOQ release.

## How it is checked

`MultisetTest` of `postgresql-starter` reads three authors and their books through `jooq-kotlin`, the author
without books included, and counts one query. Nothing makes an adapter use `MULTISET`: the test extension logs an
N+1 at `INFO` and lets the test pass.

## Consequences

- A nested read takes one query and one `mapping(::Constructor)` per level, which reads like the result it builds.
- One more artifact follows jOOQ, on the same version line, which Renovate moves together.
- The children travel as JSON inside PostgreSQL, so a decimal can lose its scale and a timestamp its zone on the way:
  each adapter test reads one back and compares it.

## Alternatives

- The core API alone: no new dependency, and the compiler checks the mapping just as well, but each level needs
  its `convertFrom` lambda.
- A join, grouped in Kotlin: plain SQL, at the cost of an outer join to keep the parents without children.
- A query per parent: the simplest code, and the N+1.

## Sources

- [The MULTISET value constructor](https://www.jooq.org/doc/3.21/manual/sql-building/column-expressions/multiset-value-constructor/), jOOQ
- [Kotlin SQL building](https://www.jooq.org/doc/3.21/manual/sql-building/kotlin-sql-building/), jOOQ
