# Database

DrinkIt keeps its data in PostgreSQL, in two schemas: `drinkit_application` for the tables of the application,
`drinkit_session` for the HTTP sessions that Spring Session stores. The schema changes through Liquibase
changelogs, SQL files that the updater, `deployment/updater`, applies apart from the application. jOOQ turns the
schema into the Kotlin classes the [adapters](../architecture/hexagonal-architecture.md) query with, committed with
the code. Until the first deployment, a change edits a file of `base/`. After it, a change is a new file in `ddl/`
or `dml/`. Either way, `./gradlew :updater:check` checks it, you apply it to your local database, then regenerate
and commit the jOOQ classes, with the commands of the [`database-change` skill][database-skill]. Read
this page before you write a changelog.

Most rules below come from one fact: a migration runs while users are still using the application. A migration that
holds a table for a few seconds too long stalls their requests, and one that fails halfway and cannot run again
blocks the next deployment. Each rule says what it protects against, so that you can tell when it really applies.

## 📜 Liquibase

Liquibase applies the changelogs, and its table `DRINKIT_DATABASECHANGELOG` records which ones each database has
received. The application never runs it, contrary to a Spring Boot default: the updater, a Spring Boot application
of its own, runs Liquibase once, then stops. This lets the application connect with a role that cannot change the
schema, while only the updater's role can. Locally, both use the `drinkit` role of `deployment/local/compose.yml`. A
changelog runs with the updater's rights, so it could widen any access: it never changes roles, privileges or
server settings, which belong to whoever runs the database.

Locally, the updater runs on each start of the backend, so a changelog you write or pull reaches your database at
the next start. [Developer experience](../writing-code/best-practices/dev-exp.md) describes the local start.

[ADR 0004](../adr/0004-one-updater-image-for-every-environment.md) records why the updater runs
from one [container image](../delivery/container-images.md) in every environment.
[ADR 0002](../adr/0002-base-ddl-and-dml-changelogs-checked-by-squawk.md) records why the changelogs are organized
as below.

### Three folders

The changelogs live in `deployment/updater/src/main/resources/db/changelog/`. `db.changelog-master.yml` runs three
folders, always in this order, and the `.sql` files of a folder in the order of their names:

| Folder | What goes there | When a file runs |
|---|---|---|
| `base/` | The schema as it stands: schemas, tables with their constraints, indexes, comments | Every time its content changes |
| `ddl/` | A change to the schema made after the base: a new column, an index, a constraint, a type change, a drop | Once |
| `dml/` | A change to the data: reference rows, a backfill, a fix | Once, after every `ddl/` file |

An empty `ddl/` or `dml/` holds a `.gitkeep`, since git keeps no empty folder: the folder shows where the first
change goes.

- **Why `base/` runs again.** While the first version of the schema takes shape, it changes often, and editing
  `base/` gives a new database the latest schema at once. Later, a [rebaseline](#rebaselining) rewrites it from the
  current schema. In both cases, the file runs again on databases that already hold the schema, so it only creates
  the objects that are missing: it must change nothing there and block no read or write.
  `CREATE TABLE IF NOT EXISTS` skips a table that exists, so a column added to it in `base/` reaches new databases
  only.
- **Where a schema change goes today.** No database is deployed yet, in October 2026. Until the first deployment,
  edit `base/` and recreate your local database. After it, write the change in `ddl/`.
- **Why `ddl/` and `dml/` run once.** A file that runs again takes its locks again, even when every statement is
  guarded by `IF NOT EXISTS`. A new file per change also tells which databases have received it. Liquibase refuses
  a file whose content changed after it ran, since its checksum no longer matches: a fix goes in the next file.
- **Why data comes after the schema.** `dml/` runs after every `ddl/` file, so a data change can use a column added
  in the same deployment. A schema change that needs the data fixed first, such as a constraint that old rows break,
  waits for the deployment after the fix. Keeping data apart also shows a reviewer which files touch the rows of
  the users.

### File names and header

A `base/` file is named after its level, a two-digit rank: `<two digits>-<name>.sql`. Lower levels run first, so a
file may depend on the files of a lower level. Two files of one level, such as `01-initial-schema.sql` and
`01-spring-sessions.sql`, run in an order that only follows their names, so neither may depend on the other.

A `ddl/` or `dml/` file is named after its order, `<four digits>-<change>.sql`, with a number no other file of its
folder uses, such as `0001-add-cellar-description.sql`.

Every file starts the same way:

```sql
--liquibase formatted sql
--changeset <author>:0001-add-cellar-description logicalFilePath:fixed runInTransaction:false

SET lock_timeout = '5s';
SET statement_timeout = '1min';
```

1. The header line. Without it, Liquibase runs the file as plain SQL and ignores every option of its changeset.
2. The single changeset of the file, named after the file, with the options below: a `base/` file adds
   `runOnChange:true`. The author is free: the existing files use the maintainer's GitHub login.
3. The two timeouts, before the first statement, as [Bounded waits](#bounded-waits) explains.

| Option | Where | Why |
|---|---|---|
| `logicalFilePath:fixed` | Every file | Liquibase identifies a changeset by its id, its author and its file path. The literal path `fixed` keeps a moved file from running again. A renamed file changes its id, so it would run again: never rename a file that has run |
| `runInTransaction:false` | Every file | Each statement commits on its own, so a lock lasts one statement instead of the whole file, and `CREATE INDEX CONCURRENTLY`, which refuses a transaction, can run. A file that fails halfway then runs again from its first statement, which is why every statement is idempotent |
| `runOnChange:true` | `base/` only | The file runs again when its content changes |
| `splitStatements` | Left to its default, `true` | Liquibase splits the file and sends one statement at a time, the same statements that Squawk, the SQL linter of the [checked rules](#checked-rules), checks. Liquibase's splitter also cuts a `$$` body at an inner `END IF;`, so a `DO` block, which Squawk cannot read anyway, would break |

### Rebaselining

When `ddl/` holds many files that every environment has run, rewrite `base/` from the current schema and delete
those files. A new database then builds its schema from a few readable files, and the history stays short.

1. **Pick the cutoff**: the `ddl/` files that ran in every environment, production and every developer database
   included. Never cut a file that may still be pending somewhere: that database would never receive its change.
2. **Build a database** from `base/` and the `ddl/` files up to the cutoff, then dump each schema:
   `pg_dump --schema-only --schema=drinkit_application`, then the same for `drinkit_session`.
3. **Rewrite each `base/` file** from the dump, in the form `base/` asks: the timeouts first,
   `CREATE ... IF NOT EXISTS`, indexes `CONCURRENTLY`. Each constraint goes inside its `CREATE TABLE`, since `base/`
   allows no `ALTER TABLE`.
4. **Delete the cut `ddl/` files.** Keep a `dml/` file whose rows a new database still needs, such as reference
   data. A one-off backfill can go.
5. **Compare** a new database built from the new files with an existing one: both `pg_dump --schema-only` must be
   identical.

The rebaseline is safe for existing databases. The new `base/` runs again there and finds every object in place,
so each statement changes nothing and blocks no read or write. Liquibase ignores the rows of
`DRINKIT_DATABASECHANGELOG` whose file is gone.

## 🗄️ Schema conventions

- **A schema of its own.** The tables live in `drinkit_application`, never in `public`, the default schema of every
  database, shared with extensions and other tools. A schema of its own keeps DrinkIt's objects apart, and a role
  gets its rights on all of them at once. Name the schema in every statement too: PostgreSQL otherwise picks the
  table through the `search_path`, which may not be the one you meant.
- **`TIMESTAMP WITH TIME ZONE` for a moment, `DATE` for a calendar date** such as a birth date. A plain `TIMESTAMP`
  drops the offset it was given, so `10:00+02` and `10:00-06` are stored as the same value. A `DATE` holds no time,
  so no time zone can move it to the day before.
- **Lowercase `snake_case` names, under 63 characters.** PostgreSQL folds unquoted names to lowercase, so a capital
  letter or a hyphen forces quotes around the name in every query. It also silently cuts a longer name, so two cut
  names can collide.
- **Types that do not run out or surprise.**
  - `BIGINT` for identifiers and counters: an `INT` runs out at 2.1 billion, and widening it rewrites the table.
  - An identity column rather than `SERIAL`, whose sequence lives apart from its table and complicates permissions
    and copies.
  - No `CHAR(n)`, which pads its values with spaces and compares them in surprising ways.
  - A `VARCHAR(n)` when its length is a rule of the data, since it is often the only guard on it. Widening it later
    only changes the catalog.

## 🛡️ Writing safe migrations

### Why locks matter

A schema change usually needs an `ACCESS EXCLUSIVE` lock, which conflicts with every other lock, reads included.
While it waits for a long query to finish, every query that arrives after it queues behind it. The table then looks
down for every user, although the change itself would take a millisecond. The rules below keep each lock short and
bounded in time.

A statement guarded by `IF NOT EXISTS` is no shelter: measured on PostgreSQL 18.6, most still wait for their lock
before they find nothing to do.

- `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` waits for an `ACCESS EXCLUSIVE` lock when the column exists.
- `CREATE INDEX IF NOT EXISTS` waits for a `SHARE` lock, which blocks writes.
- `CREATE TABLE IF NOT EXISTS` waits for nothing.
- `CREATE INDEX CONCURRENTLY IF NOT EXISTS` and `COMMENT ON` take a `SHARE UPDATE EXCLUSIVE` lock, which lets reads
  and writes through. They wait behind a running `VACUUM`, `ANALYZE` or index build, at most `lock_timeout`, and
  block no user meanwhile. An autovacuum gives way to them at once, a manual `VACUUM` does not.

This is why `base/` keeps to `CREATE TABLE IF NOT EXISTS`, `CREATE INDEX CONCURRENTLY IF NOT EXISTS` and
`COMMENT ON`: running it again never blocks a user.

### Bounded waits

`SET lock_timeout = '5s'` turns a long wait for a lock into a failure after five seconds: the updater stops with an
error, and you run it again later, rather than stalling the application. A statement that runs for too long holds
its locks as long, and `SET statement_timeout = '1min'` stops it. A file that needs more, an index on a large table
for instance, raises `statement_timeout` right before that statement.

A `SET` lasts until the end of the Liquibase run, into the next files. Every file sets the two timeouts first, so a
raised timeout ends with its file. A file that changes any other setting resets it with `RESET`.

### Idempotency

Every statement must be idempotent: run again, it changes nothing. Three situations run a statement again: a file
that failed halfway runs again from its first statement, `base/` runs again when it changes, and the local apply
command of the [`database-change` skill][database-skill] replays every file. These forms check before
they act:

- `CREATE TABLE IF NOT EXISTS ...`
- `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...`
- `CREATE INDEX CONCURRENTLY IF NOT EXISTS ...`
- `DROP ... IF EXISTS ...`
- `INSERT ... ON CONFLICT DO NOTHING`, for data

When a statement has no `IF NOT EXISTS` form, `ADD CONSTRAINT` for instance, drop the object with `IF EXISTS`
first, as [Foreign keys](#foreign-keys) shows.

### Indexes

A plain `CREATE INDEX` or `DROP INDEX` blocks the writes to the table while it runs, minutes on a large table.

- **Build concurrently**, with `CREATE INDEX CONCURRENTLY IF NOT EXISTS`, on a new table too. Squawk accepts a plain
  build on a new table only inside a transaction, and every file runs outside one. In `base/`, the statement also
  runs again once the table holds rows.
- **Drop before you build, in `ddl/`.** A concurrent build that stops halfway, on a timeout or a duplicate value,
  leaves an invalid index behind. When the file runs again, `IF NOT EXISTS` finds that index and skips the build:
  the index stays invalid for good, and no query uses it. Dropping it right before the build rebuilds it after a
  failed run:

  ```sql
  DROP INDEX CONCURRENTLY IF EXISTS drinkit_application.cellar_name_idx;
  CREATE INDEX CONCURRENTLY IF NOT EXISTS cellar_name_idx
      ON drinkit_application.cellar (name);
  ```

  The index takes the schema of its table: `CREATE INDEX` refuses a qualified name. `base/` does not drop first,
  since it runs again on every change and would rebuild every index each time. After
  a failed run of `base/`, drop the invalid index by hand before running it again.
- **Drop concurrently**, with `DROP INDEX CONCURRENTLY IF EXISTS`.
- **A long build in a file of its own**, with a raised `statement_timeout`. The raised timeout then covers that build
  alone, and a failed build runs again without the statements around it.
- **Indexes before their column.** Drop the indexes of a column with `DROP INDEX CONCURRENTLY` first. `DROP COLUMN`
  would drop them too, under its `ACCESS EXCLUSIVE` lock, which then lasts longer.

### Foreign keys

Adding a foreign key to an existing table checks every row under a lock that blocks its writes. Add it `NOT VALID`,
which checks the new rows only, then validate it in a later file, under a lock that lets reads and writes through.
A validation that times out then runs again alone, without dropping and adding the constraint again. When existing
rows may break it, fix them first in a `dml/` file. `dml/` runs after every `ddl/` file, so the validation goes in a
`ddl/` file of the next deployment.

```sql
-- Adding the constraint again after a failed run keeps the file idempotent
-- squawk-ignore ban-drop-constraint
ALTER TABLE drinkit_application.tasting DROP CONSTRAINT IF EXISTS tasting_cellar_id_fkey;
ALTER TABLE drinkit_application.tasting ADD CONSTRAINT tasting_cellar_id_fkey
    FOREIGN KEY (cellar_id) REFERENCES drinkit_application.cellar (id) NOT VALID;
```

```sql
-- Validating a valid constraint again changes nothing
-- squawk-ignore prefer-robust-stmts
ALTER TABLE drinkit_application.tasting VALIDATE CONSTRAINT tasting_cellar_id_fkey;
```

Index the referencing columns: a delete in the referenced table looks them up, and scans the whole table without an
index. A `UNIQUE` constraint follows the same idea as a foreign key: build its index concurrently, then
`ADD CONSTRAINT ... UNIQUE USING INDEX`, which takes the existing index instead of building one under lock.

### NOT NULL constraints

`SET NOT NULL` on a large table locks it while it scans every row. Proceed in three steps, each in a file of its own
for the reason given in [Foreign keys](#foreign-keys). When the second step needs a `dml/` file, the third waits for
the next deployment.

1. **Add a `CHECK (column IS NOT NULL)` constraint `NOT VALID`.** It is added at once, since PostgreSQL does not
   check the existing rows. Drop it with `IF EXISTS` first, as for [Foreign keys](#foreign-keys).
2. **Leave no `NULL` in the column**: check it, or fill the gaps from a `dml/` file.
3. **Validate the constraint**, under a lock that lets reads and writes through:

   ```sql
   ALTER TABLE drinkit_application.tasting VALIDATE CONSTRAINT tasting_rating_not_null;
   ```

The validated constraint keeps `NULL` out, and the column itself stays nullable: Squawk's
`adding-not-nullable-field` flags a `SET NOT NULL` and asks for this constraint instead.

### A column with a default

A constant default, or a stable one such as `now()`, adds the column without rewriting the table. A volatile default
such as `gen_random_uuid()` rewrites it under lock: add the column without a default, set the default, then fill the
existing rows in batches from `dml/`.

### Changing a column type

Some changes only update the catalog: widening a `VARCHAR(n)`, `VARCHAR` to `TEXT`, and `TIMESTAMP` to
`TIMESTAMP WITH TIME ZONE` when the session runs in UTC. Squawk still flags them: tolerate its rule, with that
reason.

```sql
SET TimeZone = 'UTC';
-- Under UTC this converts the catalog only, and running it again on TIMESTAMP WITH TIME ZONE changes nothing
-- squawk-ignore changing-column-type, prefer-robust-stmts
ALTER TABLE drinkit_application.tasting ALTER COLUMN tasted TYPE TIMESTAMP WITH TIME ZONE;
RESET TimeZone;
```

Any other type change rewrites the whole table under lock. Move the data to a new column instead:

1. Add a new column with the new type.
2. Copy each `INSERT` and `UPDATE` from the old column to the new one with a trigger. Its function needs a `$$` body
   that Liquibase splits wrongly, so writing the first one needs a decision that
   [ADR 0002](../adr/0002-base-ddl-and-dml-changelogs-checked-by-squawk.md) leaves open.
3. Copy the existing rows in batches, from a `dml/` file.
4. Switch the application code to the new column.
5. Once that code is deployed and stable, drop the old column and the trigger in a later file.

### Renaming and dropping

During a deployment, the previous version of the application keeps running until the new one replaces it, and it
still uses the old names. A rename breaks it at once. Change names in steps, which is called
[expand and contract](../writing-code/best-practices/coding-rules.md#evolve-a-contract-in-steps):

1. **Expand**: add the new column or table, and write to both.
2. **Move**: copy the existing data in a `dml/` file, then switch the code to the new name.
3. **Contract**: once no running version uses the old name, drop it in a later `ddl/` file.

A drop destroys data that no rollback brings back. Squawk refuses every drop until a `-- squawk-ignore` says, on the
line above, why the data is no longer needed.

### Changing data

- **Idempotent.** Write `INSERT ... ON CONFLICT DO NOTHING`, or an `UPDATE` whose `WHERE` skips the rows already
  done, so that running it again changes nothing.
- **In batches.** An `UPDATE` of many rows holds the lock of each row until it ends, and leaves as many dead rows to
  clean. Split a large backfill into several statements, each bounded by a range of ids of a few thousand rows.

## 🔍 Checked rules

`./gradlew :updater:check`, the check task of `deployment/updater` and part of `./gradlew build`, runs two checks
and fails on any finding. Gradle downloads the Squawk binary of your platform and checks its checksum, so the check
needs nothing beyond the JDK.

- `ChangelogConventionsTest` reads every changelog with Liquibase's own parser, and checks how the folders and
  changesets are written.
- [Squawk](https://squawkhq.com/docs/rules), a linter for PostgreSQL migrations, reads their SQL.
  `deployment/updater/.squawk.toml` lists every one of its rules, on or off, so that nobody has to know its defaults.

In CI, the Updater lane of the [CI](../delivery/ci-cd.md) runs the same command, then puts each Squawk finding on its
line, in the "Files changed" tab of the pull request.

### What a failure looks like

A rule of `ChangelogConventionsTest` names itself, says why it exists and how to fix it, then lists every file that
breaks it:

```
ChangelogConventionsTest > every base migration only creates what is missing() FAILED
    Rule base-creates-only: base/ runs again on existing databases, where each statement must change nothing
    and block no read or write. Fix: keep to `SET`, `CREATE ... IF NOT EXISTS`, `CREATE INDEX CONCURRENTLY IF NOT EXISTS`
    and `COMMENT ON`, and write any other change in a ddl/ file. See "Checked rules" in docs/src/engineering/build-and-data/database.md.
    expected:<[]> but was:<["base/02-cellar-rooms.sql"]>
```

Squawk shows the rule with a link to its page, the statement, and the fix it suggests. Gradle then says what to do
next:

```
warning[prefer-timestamp-tz]: When Postgres stores a datetime in a `timestamp` field, Postgres drops the UTC offset.
  ╭▸ src/main/resources/db/changelog/ddl/0001-create-tasting.sql:7:73
7 │ CREATE TABLE drinkit_application.tasting (id BIGINT PRIMARY KEY, tasted TIMESTAMP NOT NULL);
  ├ help: Use `timestamptz` instead of `timestamp` for your column type.
Found 2 issues in 1 file (checked 4 source files)

Execution failed for task ':updater:squawk'.
> Squawk failed on the changelogs, as printed above. A finding names its rule, links to its page and shows a fix.
```

### Tolerating a rule

A rule that does not apply to one statement is tolerated in its file, never for the whole project, so that it keeps
checking every other statement. `-- squawk-ignore <rule>` covers the next statement, `-- squawk-ignore-file <rule>`
the whole file. The directive stands on a line of its own, right below a comment that says why the rule does not
apply, as the examples of [Foreign keys](#foreign-keys) show.

### Rules of `ChangelogConventionsTest`

| Rule | What it asks | Why |
|---|---|---|
| `formatted-sql-header` | The first line is `--liquibase formatted sql` | Without it, Liquibase runs the file as plain SQL and ignores its options |
| `one-changeset-per-file` | One changeset, whose id is the file name without `.sql` | Liquibase records a run per changeset: with one per file, the file name tells which change a database has received |
| `fixed-logical-file-path` | `logicalFilePath:fixed` | A moved file would run again |
| `no-transaction` | `runInTransaction:false` | Each statement holds its locks alone, and `CONCURRENTLY` can run |
| `split-statements` | No `splitStatements:false` | Liquibase and Squawk see the same statements |
| `no-do-block` | No `DO` block | Squawk does not read the statements inside one |
| `timeouts-first` | `SET lock_timeout` and `SET statement_timeout` before any other statement | A statement that waits for its lock, or runs too long, fails instead of blocking the queries behind it |
| `squawk-ignore-reason` | Each Squawk directive on a line of its own, right below a `-- <reason>` comment | Every tolerated violation says why. Squawk also obeys a directive buried in a longer comment, where nobody would see it |
| `known-folder` | Every file directly in `base/`, `ddl/` or `dml/` | Liquibase runs the three folders in that order, and a file in a subfolder would run where its path sorts |
| `base-runs-on-change` | `runOnChange:true` in `base/` | Without it, Liquibase refuses an edited or rebaselined `base/` file, whose checksum changed |
| `base-creates-only` | Only `SET`, `CREATE ... IF NOT EXISTS`, `CREATE INDEX CONCURRENTLY IF NOT EXISTS` and `COMMENT ON` in `base/` | `base/` runs again on existing databases, where it must change nothing and block no read or write |
| `base-file-name` | A name such as `01-initial-schema.sql` | The level orders the files of `base/` |
| `run-once` | No `runOnChange` in `ddl/` and `dml/` | Running a change again takes its locks again |
| `numbered-file-name` | A name such as `0001-add-cellar-description.sql` | The number orders the files |
| `unique-file-number` | No two files of a folder share a number | Two files with one number run in an order nobody chose |
| `drop-index-before-build` | In `ddl/`, `DROP INDEX CONCURRENTLY IF EXISTS` right before each `CREATE INDEX CONCURRENTLY` of the same index | A failed build is rebuilt instead of kept as an invalid index |
| `ddl-changes-no-data` | No `INSERT`, `UPDATE`, `DELETE`, `MERGE`, `TRUNCATE` or `COPY` in `ddl/` | Data changes run after the schema, and are reviewed on their own |
| `dml-changes-no-schema` | Only `SET`, `RESET`, `INSERT`, `UPDATE`, `DELETE`, `MERGE`, `WITH` and `SELECT` in `dml/` | A schema change in `dml/` would run after the `ddl/` files and escape their rules |

### Rules of Squawk

`.squawk.toml` groups the rules by what they protect. Each rule has a page at `https://squawkhq.com/docs/<rule>`,
with examples and the safe way to write the statement.

- **No long lock, no unbounded wait.** An index built or dropped without `CONCURRENTLY` blocks writes for the whole
  build, and a lock without a timeout can stall the application: `require-lock-timeout`,
  `require-statement-timeout`, `require-concurrent-index-creation`, `require-concurrent-index-deletion`,
  `require-concurrent-reindex`, `require-concurrent-partition-detach`,
  `ban-concurrent-index-creation-in-transaction`, `prefer-repack`, `transaction-nesting`,
  `ban-uncommitted-transaction`.
- **Runs again after a failure.** A file that failed halfway runs again from its first statement:
  `prefer-robust-stmts` asks for `IF NOT EXISTS` and `IF EXISTS`.
- **No scan or rewrite of a whole table under lock.** A constraint, a required column, a volatile default or a new
  type checks or rewrites every row while the table is locked: `constraint-missing-not-valid`,
  `adding-foreign-key-constraint`, `disallowed-unique-constraint`, `adding-not-nullable-field`,
  `adding-required-field`, `adding-field-with-default`, `adding-serial-primary-key-field`, `changing-column-type`,
  `ban-create-domain-with-constraint`, `ban-alter-domain-with-add-constraint`.
- **Types and names that do not run out or surprise**, most of them explained in the schema conventions above:
  `require-table-schema`, `prefer-timestamp-tz`, `prefer-bigint-over-int`, `prefer-bigint-over-smallint`,
  `prefer-identity`, `ban-char-field`, `identifier-too-long`, `require-enum-value-ordering`, `ban-add-enum-value`,
  `ban-add-composite-attribute`.
- **The running version keeps working.** A rename, a dropped default, a changed sequence or a replaced view changes
  what the previous version reads or writes: `renaming-column`, `renaming-table`, `renaming-object`,
  `ban-set-schema`, `ban-drop-not-null`, `ban-drop-default`, `ban-set-default`, `ban-alter-identity`,
  `ban-alter-sequence-values`, `ban-alter-generated-expression`, `ban-drop-generated-expression`,
  `ban-duplicate-column-assignments`, `ban-detach-inheritance`, `ban-replace-view-function`,
  `ban-alter-view-options`, `ban-alter-function-options`, `ban-disable-trigger`, `ban-drop-trigger`.
- **Nothing disappears without a written decision.** A drop destroys data or an object for good: `ban-drop-column`,
  `ban-drop-table`, `ban-drop-schema`, `ban-drop-database`, `ban-drop-sequence`, `ban-drop-type`, `ban-drop-domain`,
  `ban-drop-view`, `ban-drop-function`, `ban-drop-extension`, `ban-drop-constraint`, `ban-truncate-cascade`.
- **Access and server settings stay with whoever runs the database.** A migration runs with elevated privileges,
  so it could widen any access: it must not change roles, privileges, replication or server settings:
  `ban-alter-database-options`, `ban-alter-system-options`, `ban-alter-role-options`, `ban-alter-extension`,
  `ban-revoke`, `ban-replica-identity`, `ban-alter-row-level-security`, `ban-create-policy`, `ban-drop-policy`,
  `ban-alter-policy-condition`, `ban-alter-policy-roles`.

Four rules are off, each for a reason written in `.squawk.toml`:

- **`prefer-text-field`.** A `VARCHAR(n)` length is a deliberate guard, often the only one, and widening it changes
  the catalog only.
- **`ban-drop-index`.** A `ddl/` file drops each index right before it builds it, so the rule would flag every
  build. A drop for good is left to the review.
- **`ban-add-column`.** Adding a nullable column is the first step of every expand and contract, and jOOQ lists the
  columns of every query it sends, so a new column changes no query.
- **`ban-new-write-restriction`.** Adding a constraint is the purpose of most `ddl/` files. The rules above check how
  it locks, and the expand and contract steps how the running version keeps working.

On a Squawk upgrade, read its changelog and add each new rule to one of the two lists of `.squawk.toml`, so that the
file still shows every rule and nobody relies on a default. Squawk fails on a misspelled or unknown name, which
catches a typo there.

### Not checked yet

Neither check starts a database, so a few rules wait for a test against PostgreSQL
([#502](https://github.com/mboisnard/drinkit/issues/502)): a primary key on each table, an index behind each foreign
key, no invalid index left behind, names without hyphens, the same schema for a new and an existing database after a
rebaseline, and no read or write blocked when `base/` runs again.

## ⚙️ jOOQ code generation

jOOQ generates Kotlin classes from the database schema, and the
[adapters](../architecture/hexagonal-architecture.md) write their SQL queries with them. The
compiler then checks each query: a renamed column fails the build instead of a request.

The classes are committed, under `src/generated/jooq/kotlin` of each module that generates them, so that the build
needs no database. Generating them in CI would mean starting PostgreSQL and applying every changelog before the
first compilation. The build never regenerates them: after a changelog change, apply it locally and regenerate them,
as the [`database-change` skill][database-skill] describes. Until then, the code compiles and the tests
run against the old schema.

## 🧪 Integration tests

An adapter's queries are tested against a real PostgreSQL, which
[Testcontainers](../writing-code/testing/testcontainers.md) starts once in Docker. `@JooqIntegrationTest` sets it
up and builds its schema from the committed jOOQ classes, not from Liquibase: a schema change is tested once those
classes are regenerated. It takes the jOOQ class of each schema
it needs, such as `DrinkitApplication` for `drinkit_application`:

```kotlin
@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqExchangeRatesTest : ExchangeRatesTestContract() {
    // ...
    override fun fetchRepository(): ExchangeRates = JooqExchangeRates(dsl = dsl, clock = ControlledClock())
}
```

The test extends the test contract of its port, an abstract class of tests that the
[in-memory double](../writing-code/testing/testdouble.md) also passes.

## 📚 Sources

- [PostgreSQL at Scale: Database Schema Changes Without Downtime](https://medium.com/paypal-tech/postgresql-at-scale-database-schema-changes-without-downtime-20d3749ed680), PayPal
- [Explicit Locking](https://www.postgresql.org/docs/current/explicit-locking.html), the lock modes and what each one blocks, PostgreSQL documentation
- [Don't Do This](https://wiki.postgresql.org/wiki/Don%27t_Do_This), the types and habits to avoid, PostgreSQL wiki
- [Migration Style Guide](https://docs.gitlab.com/development/migration_style_guide/) and [Migration Squashing](https://docs.gitlab.com/development/database/migration_squashing/), GitLab
- [Best Practices](https://docs.liquibase.com/concepts/bestpractices.html) and [Trimming ChangeLogs](https://www.liquibase.com/blog/trimming-liquibase-changelogs), Liquibase
- [Baseline Migrations](https://documentation.red-gate.com/fd/baseline-migrations-273973336.html), Flyway
- [Squawk rules](https://squawkhq.com/docs/rules), Squawk

[database-skill]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/database-change/SKILL.md
