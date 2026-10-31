---
name: database-change
description: Changes DrinkIt's PostgreSQL schema and the committed jOOQ classes generated from it, in an order that proves the changelog, the classes and the adapter agree. Use it before adding or changing a table, a column, an index or a constraint, and before touching generated jOOQ code.
paths:
  - "**/db/changelog/**"
  - "**/src/generated/jooq/**"
---

# Changing the database schema

The safe statements are in `docs/src/engineering/build-and-data/database.md`. This skill gives the order, the
commands and what proves each step.

## 1. Write the change

The "Three folders" section of `database.md` says where a change goes, `git ls-files '*/db/changelog/**/*.sql'`
lists the files, "Schema conventions" gives the schema and the names, and "Writing safe migrations" the
statements:

- a schema change is the next numbered file of `ddl/`, a data change the next one of `dml/`, and a file that has
  run never changes: Liquibase refuses it;
- `base/` changes only before the first deployment, with the local database recreated, or in a rebaseline, as
  "Rebaselining" describes.

Then run `./gradlew :updater:check`. It runs Squawk on the SQL of the changelogs and the tests of
`deployment/updater`, `ChangelogConventionsTest` on their folders, changesets and statements among them. The
Updater lane of CI runs it on every change under `deployment/updater`, where the changelogs live: the rules of the
"Checked rules" section of `database.md` are enforced by these checks, not by this skill alone. A finding names
its rule and its fix, and that section says why the rule exists.

After the file runs, a new database and an existing one must end up with the same schema. The local database is
an existing one: `deployment/local/compose.yml` keeps its data in a volume.

## 2. Apply it twice

From the root, with Docker, these commands apply every changelog in the order Liquibase runs them. Run them, then
run them a second time: an error on the second run means a statement is not idempotent, so fix it before going on.

```
docker compose -f deployment/local/compose.yml up -d postgresql
find deployment/updater/src/main/resources/db/changelog -name '*.sql' | sort | xargs cat \
  | docker compose -f deployment/local/compose.yml exec -T postgresql psql -v ON_ERROR_STOP=1 -U drinkit -d drinkit
```

## 3. Generate the jOOQ classes

A new table goes first into the `includes` of the module that holds its adapter: the `-infra` module of the
application that owns the table, the starter's own build file for a table a tech starter owns. Then run
`./gradlew jooqCodegen`. `-PjooqJdbcUrl`, `-PjooqJdbcUser` and `-PjooqJdbcPassword` point it at another database
than the local one.

## 4. Check what codegen wrote

Run `git status` and read the diff under `src/generated/jooq`. Expect changes only for the tables you touched:

- a deleted file means the database was missing the schema or the table;
- a change to a table you did not touch means the local database differs from the changelogs, for instance
  after work on another branch.

In both cases, `git restore` the generated directory, bring the database in line with step 2, and generate
again. Commit the classes with the changelog that produced them, so every commit compiles.

## 5. Test the adapter

The adapter test runs the port's `*TestContract` against PostgreSQL with `@JooqIntegrationTest`, as
`JooqExchangeRatesTest` does. It builds the test database from the committed classes, not from Liquibase: a green
adapter test proves the code against the classes, and steps 2 and 4 are what prove the classes match the
changelog. Never skip them.

## Before you finish

- [ ] The change sits in the right folder, the statements follow "Writing safe migrations",
  `./gradlew :updater:check` passes, and the apply commands ran twice without error.
- [ ] A new table is in the `includes` of its module.
- [ ] `git status` shows no deleted generated class and no change to a table outside the issue.
- [ ] The adapter test passes.
