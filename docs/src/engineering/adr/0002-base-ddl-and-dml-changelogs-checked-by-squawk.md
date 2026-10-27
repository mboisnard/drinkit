# 0002. Base, DDL and DML changelogs, checked by Squawk

- Status: accepted
- Date: 2026-10-24
- Issue: [#399](https://github.com/mboisnard/drinkit/issues/399)

## Context

The schema lived in one Liquibase file per schema in `base/`, run again on each change of the file (`runOnChange:true`), and sent as one statement (`splitStatements:false`). Nothing checked the conventions of `database.md`. #399 asked for a check on each build, and for the safest migrations an application with many users needs.

Three facts, measured on PostgreSQL 18.6 and Liquibase 5.0.3, shaped the decision:

- A statement run again waits for its lock even when its object exists: `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` for an `ACCESS EXCLUSIVE` lock, `CREATE INDEX IF NOT EXISTS` for a `SHARE` lock. `CREATE TABLE IF NOT EXISTS` waits for nothing, and `CREATE INDEX CONCURRENTLY IF NOT EXISTS` and `COMMENT ON` only for a `SHARE UPDATE EXCLUSIVE` lock, which lets reads and writes through.
- Squawk, the PostgreSQL migration linter, does not read the statements inside a `DO` block, the usual guard of a statement that has no `IF NOT EXISTS` form.
- Liquibase's statement splitter cuts a `$$` body at an inner `END IF;`.

## Decision

The changelogs live in three folders, which Liquibase runs in this order:

- `base/` holds the schema, runs again when it changes, and only creates what is missing, so that running it again changes nothing and blocks no read or write. When `ddl/` grows, a rebaseline rewrites `base/` from the current schema and deletes the `ddl/` files every environment has run.
- `ddl/` holds the later schema changes, one numbered file per change, run once.
- `dml/` holds the data changes, one numbered file per change, run once, after every `ddl/` file.

Every file runs outside a transaction, its statements split by Liquibase, with no `DO` block, and sets `lock_timeout` and `statement_timeout` first. A `ddl/` file drops an index right before it builds it, so that an interrupted build is not kept as an invalid index.

`./gradlew :updater:check`, part of the build, runs two checks without a database. `ChangelogConventionsTest` reads the files with Liquibase's own parser for the rules of each folder. Squawk lints the SQL, with `.squawk.toml` listing each of its rules on or off: Gradle downloads its binary for the platform from Squawk's GitHub releases, checked against its checksum. An exception to a rule stands in its file, below its reason. A CI lane of its own, Updater, runs the same command.

## Consequences

- `base/` shows the whole schema, and a new database builds it from a few files. An edit to an existing table in `base/` reaches new databases only: before the first deployment, a local database is recreated, and after it the change goes in `ddl/`.
- A `ddl/` or `dml/` file cannot change once it has run, since Liquibase refuses a new checksum: a fix is the next file.
- A schema change that needs data first waits for the next deployment, since `dml/` runs after `ddl/`.
- A rebaseline is safe for existing databases, which run the new `base/` without effect, but only for `ddl/` files every environment has run. Comparing the two schemas stays manual until a test against PostgreSQL does it (#502).
- Squawk's GitHub releases become a source of the build, after Maven Central. The build needs no Node, and Alpine, with musl, gets no binary.
- A trigger function needs a `$$` body that Liquibase splits wrongly, so the first one needs a decision.
- Rules on the catalog, such as a primary key on each table or an index behind each foreign key, wait for a test against PostgreSQL (#502).

## Alternatives

- One numbered file per change, `base/` included, none run again: Squawk sees every statement, but no file shows the schema whole, and a new database replays the whole history.
- One file per schema, run again on change, its `ALTER TABLE` and `CREATE INDEX` guarded in `DO` blocks: no blocking lock on a rerun, but Squawk sees none of the guarded statements.
- Liquibase's policy checks: they need a Pro license.
- Regular expressions over the SQL text: they also match inside comments and strings, and each rule needs its own expression.
- sqlfluff: its rules cover style, not the locks a statement takes.
- Squawk through its Docker image: it has no arm64 build.
- Squawk through npm: its `squawk-cli` package is a Node launcher of the same binaries, and a Node missing from the `PATH` of an IDE failed the build with a message that blamed the changelogs ([#505](https://github.com/mboisnard/drinkit/issues/505)).
- Squawk through npm, with a clear error on a missing Node: about ten lines, but the build kept needing Node, and an IDE started without it kept failing.
- The `com.github.node-gradle.node` plugin, which downloads Node: an open issue with the configuration cache when it downloads Node, no support for `.nvmrc`, and an archive of Node to verify per platform, for one linter.
