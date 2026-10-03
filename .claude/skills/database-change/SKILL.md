---
name: database-change
description: Changes DrinkIt's PostgreSQL schema and the committed jOOQ classes generated from it, in an order that proves the changelog, the classes and the adapter agree. Use it before adding or changing a table, a column, an index or a constraint, and before touching generated jOOQ code.
paths:
  - "**/db/changelog/**"
  - "**/src/generated/jooq/**"
---

# Changing the database schema

The commands are in the "Database and jOOQ" section of `AGENTS.md`, the safe statements in
`docs/src/engineering/guidelines/database.md`. This skill gives the order and what proves each step.

## 1. Edit the changelog

Edit the file of the schema in place, as the "Writing Safe Migrations" section of `database.md` says. The
application tables live in `drinkit_application`:
`git grep -l 'CREATE SCHEMA IF NOT EXISTS drinkit_application' -- '*.sql'` finds its file.

After the file runs, a new database and an existing one must end up with the same schema. The local database is
an existing one: `deployment/local/compose.yml` keeps its data in a volume.

## 2. Apply it twice

Run the apply commands of `AGENTS.md`, then run them a second time. An error on the second run means a statement
is not idempotent: fix it before going on.

## 3. Generate the jOOQ classes

A new table goes first into the `includes` of the module that holds its adapter: `drinkit-infra` for the
application, the starter's own build file for a table a tech starter owns. Then run `./gradlew jooqCodegen`.
`-PjooqJdbcUrl`, `-PjooqJdbcUser` and `-PjooqJdbcPassword` point it at another database than the local one.

## 4. Check what codegen wrote

Run `git status` and read the diff under `src/generated/jooq`. Expect changes only for the tables you touched:

- a deleted file means the database was missing the schema or the table;
- a change to a table you did not touch means the local database differs from the changelogs, for instance
  after work on another branch.

In both cases, `git restore` the generated directory, bring the database in line with step 2, and generate
again. Commit the classes with the changelog that produced them, so every commit compiles.

## 5. Test the adapter

The adapter test runs the port's `*TestContract` against PostgreSQL with `@JooqIntegrationTest`, whose model
is in the examples of the `hexagonal-backend` skill. It builds the test database from the committed classes,
not from Liquibase: a green adapter test proves the code against the classes, and steps 2 and 4 are what prove
the classes match the changelog. Never skip them.

## Before you finish

- [ ] The statements follow "Writing Safe Migrations", and the apply commands ran twice without error.
- [ ] A new table is in the `includes` of its module.
- [ ] `git status` shows no deleted generated class and no change to a table outside the issue.
- [ ] The adapter test passes.
