# Database

This page explains how we use **PostgreSQL** database with **jOOQ** for code generation and **Liquibase** for managing migrations in this project.
This approach ensures reliable deployments, safe migrations, and a smooth integration between the database and the application code.

Most rules below come from one fact: a migration runs while users are still using the application. A migration that holds a table for a few seconds too long stalls their requests, and one that cannot run twice blocks the next deployment. Each rule says what it protects against, so that you can tell when it really applies.

## 🐘 Liquibase

Liquibase is an essential tool for tracking, managing, and applying database schema changes in a platform-independent way. It enables versioned, reproducible, and team-friendly migrations. [ADR 0002](../adr/0002-base-ddl-and-dml-changelogs-checked-by-squawk) records why the changelogs are organized as below.

### 📂 Three Folders

The changelogs live in `deployment/updater/src/main/resources/db/changelog/`. `db.changelog-master.yml` runs three folders, always in this order, and the files of a folder in the order of their names:

| Folder | What goes there | When a file runs |
|---|---|---|
| `base/` | The schema as it stands: schemas, tables with their constraints, indexes, comments | Every time its content changes |
| `ddl/` | A change to the schema made after the base: a new column, an index, a constraint, a type change, a drop | Once |
| `dml/` | A change to the data: reference rows, a backfill, a fix | Once, after every `ddl/` file |

- **Why `base/` runs again.** While the first version of the schema takes shape, it changes often, and editing `base/` gives a new database the latest schema at once. Later, a [rebaseline](#rebaselining) rewrites it from the current schema. In both cases, the file runs again on databases that already hold the schema, so it must change nothing there and block no read or write: `base/` only creates what is missing. An edit to an existing table in `base/` therefore reaches new databases only. Before the first deployment, recreate your local database. After it, write the change in `ddl/`.
- **Why `ddl/` and `dml/` run once.** A file that runs again takes its locks again, even when every statement is guarded by `IF NOT EXISTS`. A new file per change also tells which databases have received it. Liquibase refuses a file whose content changed after it ran, since its checksum no longer matches: a fix goes in the next file.
- **Why data comes after the schema.** `dml/` runs after every `ddl/` file, so a data change can use a column added in the same deployment. A schema change that needs data first, such as a constraint after a cleanup or a `NOT NULL` after a backfill, waits for the next deployment, as the steps below already require. Keeping data apart also shows a reviewer at a glance which files touch the rows of the users.

A `base/` file is named after its level, `<two digits>-<name>.sql`. Files of one level do not depend on each other, such as `01-initial-schema.sql` and `01-spring-sessions.sql`. A `ddl/` or `dml/` file is named after its order, `<four digits>-<change>.sql`, with a number no other file of its folder uses, such as `0001-add-cellar-description.sql`. Every file starts the same way:

1.  **Liquibase Header**: The script must begin with the following line, otherwise Liquibase runs the file as plain SQL and ignores every option of its changeset:
    ```sql
    --liquibase formatted sql
    ```

2.  **Changeset Description**: Immediately after, the single changeset of the file, named after the file. A `base/` file adds `runOnChange:true`.
    ```sql
    --changeset mboisnard:0001-add-cellar-description logicalFilePath:fixed runInTransaction:false
    ```

3.  **Timeouts**: Before the first statement, as [Bounded Waits](#bounded-waits) explains.
    ```sql
    SET lock_timeout = '5s';
    SET statement_timeout = '1min';
    ```

### 🧰 Useful Changeset Options

-   `logicalFilePath:fixed`: **Mandatory**. Liquibase identifies a changeset by its id, its author and its file path. Fixing the path keeps a moved or renamed file from running again.
-   `runInTransaction:false`: **Mandatory**. Each statement commits on its own, so a lock lasts one statement instead of the whole file, and `CREATE INDEX CONCURRENTLY`, which refuses to run in a transaction, can run. A file that fails halfway runs again from its first statement, which is why every statement is idempotent.
-   `runOnChange:true`: **In `base/` only**. The file runs again when its content changes.
-   `splitStatements`: Left to its default, so Liquibase sends the statements one by one, as Squawk reads them. Its splitter cuts a `$$` body at an inner `END IF;`, which is one reason a changelog holds no `DO` block.
-   `labels:local`: Allows you to run a script only in a specific environment (e.g., `local` for development).

### ♻️ Rebaselining {#rebaselining}

When `ddl/` holds many files that every environment has run, rewrite `base/` from the current schema and delete those files. A new database then builds its schema from a few readable files, and the history stays short. GitLab does the same a few times a year, folding its old migrations into one structure file, and Flyway calls such a file a baseline migration.

1.  **Pick the cutoff**: the `ddl/` files that ran in every environment, production and every developer database included. Never cut a file that may still be pending somewhere: that database would never receive its change.
2.  **Build a database** from `base/` and the `ddl/` files up to the cutoff, then dump its schema, one schema at a time: `pg_dump --schema-only --schema=drinkit_application`.
3.  **Rewrite each `base/` file** from the dump, in the form its rules ask: `CREATE ... IF NOT EXISTS`, each constraint inside its `CREATE TABLE`, indexes `CONCURRENTLY`, the timeouts first.
4.  **Delete the cut `ddl/` files.** Keep a `dml/` file whose rows a new database still needs, such as reference data. A one-off backfill can go.
5.  **Compare** a new database built from the new files with an existing one: both `pg_dump --schema-only` must be identical.

The rebaseline is safe for existing databases. The new `base/` runs again there and finds every object in place, so each statement changes nothing and blocks no read or write. Liquibase ignores the rows of `DATABASECHANGELOG` whose file is gone.

### 🔒Security: Liquibase Outside the Application

To enhance security, it is recommended to run Liquibase via an external process (CI/CD, dedicated script) with elevated privileges, while the application connects to the database using a role with restricted permissions (read/write on its tables, but no administrative rights like `CREATE`, `ALTER`, `DROP`). For the same reason, a changelog never changes roles, privileges or server settings: they belong to the people who run the database.

## ✅ Database Best Practices

- **Use a Dedicated Schema**: To avoid conflicts and keep your database well-organized, it is crucial not to use the default `public` schema. Create a specific schema for your application, such as `drinkit_application`. This isolates your database objects and simplifies permissions management. Name the schema in every statement too: PostgreSQL otherwise picks the table through the `search_path`, which may not be the one you meant.


- **Don't ignore timezones when manipulating dates and times**: don't use `TIMESTAMP` alone use `TIMESTAMP WITH TIME ZONE` unless you actually want to store a logical date such as a birth date, in which case you should use `DATE`. A `TIMESTAMP` drops the offset it was given: `10:00+02` and `10:00-06` are stored as the same value although they are eight hours apart.


- **Don't use hyphens (`-`) to name your PostgreSQL objects**: It's just painful to have to protect every such name with quotes. Prefer lowercase `snake_case`, which needs no quotes either, and keep names under 63 characters: PostgreSQL silently cuts longer ones, and two cut names can then collide.


- **Pick types that will not run out or surprise**:
  - `BIGINT` for identifiers and counters: an `INT` stops at 2.1 billion, and widening it later rewrites the table.
  - An identity column rather than `SERIAL`, whose sequence lives apart from its table and complicates permissions and copies.
  - No `CHAR(n)`, which pads its values with spaces and compares them in surprising ways.
  - A `VARCHAR(n)` is fine when its length is a rule of the data, since it is often the only guard on it. Widening it later only changes the catalog.

## 🛡️Writing Safe Migrations

### Why Locks Matter 🔐

PostgreSQL takes a lock on a table for each statement. Most locks share well: reads and writes run side by side. A schema change usually needs an `ACCESS EXCLUSIVE` lock, which shares with nothing. While it waits for a long query to finish, every query that arrives after it queues behind it, reads included. The table then looks down for every user, although the change itself would take a millisecond.

The rules below keep each lock short, bounded in time, and away from statements that do not need it. Measured on PostgreSQL 18.6, even a statement guarded by `IF NOT EXISTS` takes its lock before it finds nothing to do: `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` waits for an `ACCESS EXCLUSIVE` lock when the column exists, and `CREATE INDEX IF NOT EXISTS` for a `SHARE` lock that blocks writes. `CREATE TABLE IF NOT EXISTS` waits for nothing. `CREATE INDEX CONCURRENTLY IF NOT EXISTS` and `COMMENT ON` take a `SHARE UPDATE EXCLUSIVE` lock, which lets reads and writes through, but waits behind a running `VACUUM`, `ANALYZE` or index build, at most `lock_timeout`. An autovacuum gives way to it at once, a manual `VACUUM` does not. This is why `base/` keeps to these statements: running it again never blocks a user.

### Bounded Waits ⏱️ {#bounded-waits}

`SET lock_timeout = '5s'` turns a long wait for a lock into a failure after five seconds: the deployment fails and runs again later, rather than stalling the application. `SET statement_timeout = '1min'` bounds a statement that runs for too long. A file that needs more, an index on a large table for instance, raises `statement_timeout` right before that statement. A `SET` lasts until the end of the Liquibase run, into the next files: a file resets any other setting it changes, with `RESET`.

### The Golden Rule: Idempotency 🔄

The fundamental rule is that **every statement must be idempotent**.
An idempotent operation can be executed multiple times without changing the result beyond its initial application.

Three situations run a statement again: a file that failed halfway runs again from its first statement, `base/` runs again when it changes, and the local apply command of `AGENTS.md` replays every file. To achieve this, use SQL constructs that check for existence before creating or modifying:
-   `CREATE TABLE IF NOT EXISTS ...`
-   `ALTER TABLE ... ADD COLUMN IF NOT EXISTS ...`
-   `CREATE INDEX CONCURRENTLY IF NOT EXISTS ...`
-   `DROP ... IF EXISTS ...`
-   `INSERT ... ON CONFLICT DO NOTHING`, for data

When a statement has no `IF NOT EXISTS` form (`ADD CONSTRAINT` for instance), drop the object with `IF EXISTS` first, as [Foreign Keys](#foreign-keys) shows.

A new database and an existing one end up with the same schema, since both run the same files in the same order.

### Handling Complex Schema Changes (Without Locking) ⚠️

Certain database operations can lock tables for a long time (`ACCESS EXCLUSIVE lock`), which is unacceptable in production. Here is how to handle the most common cases.

#### Creating/Dropping Indexes 🔍

A standard index creation or deletion blocks the writes to the table while it runs, minutes on a large table.

-   **Creation**: Always `CREATE INDEX CONCURRENTLY IF NOT EXISTS`, on a new table too. It builds the index while writes go on.
-   **The invalid index trap**: A concurrent build that stops halfway, on a timeout or a duplicate value, leaves an invalid index behind. When the file runs again, `IF NOT EXISTS` finds that index and skips the build: the index stays invalid for good, and no query uses it. In `ddl/`, drop the index right before building it, which rebuilds it after a failed run:
    ```sql
    DROP INDEX CONCURRENTLY IF EXISTS drinkit_application.cellar_name_idx;
    CREATE INDEX CONCURRENTLY IF NOT EXISTS cellar_name_idx
        ON drinkit_application.cellar (name);
    ```
    `base/` does not drop first, since it runs again on every change and would rebuild every index each time. After a failed run of `base/`, drop the invalid index by hand before running it again.
-   **Deletion**: Use `DROP INDEX CONCURRENTLY IF EXISTS`.
-   **Large table**: An index that takes minutes to build goes in a file of its own, with a raised `statement_timeout`.
-   **Tip**: Before dropping a column (`DROP COLUMN`), it is better to first drop any associated indexes using `DROP INDEX CONCURRENTLY` to minimize the duration of the `ACCESS EXCLUSIVE` lock on the table.

#### Foreign Keys 🔗 {#foreign-keys}

Adding a foreign key to an existing table checks every row under a lock that blocks its writes. Add it `NOT VALID`, which checks the new rows only, then validate it in the next file, under a lock that lets reads and writes through. Index the referencing columns: a delete in the referenced table looks them up, and scans the whole table without an index.

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

A `UNIQUE` constraint follows the same idea: build its index concurrently, then `ADD CONSTRAINT ... UNIQUE USING INDEX`, which takes the existing index instead of building one under lock.

#### NOT NULL Constraint 🚫

Adding a `NOT NULL` constraint on a large table locks it while it scans all rows. To avoid this, proceed in three steps (in separate files, each applied before the next):

1.  **Add a non-validated `CHECK` constraint**: It is added instantly because the database does not check existing data. Dropping it first keeps the file idempotent, as for [Foreign Keys](#foreign-keys).
2.  **Manually verify** that there are no longer any `NULL` values in the column, or fill them in a `dml/` file.
3.  **Validate the constraint**: This operation only requires a light lock to update metadata.
    ```sql
    ALTER TABLE my_table VALIDATE CONSTRAINT my_column_not_null;
    ```

#### Adding a Column With a Default ➕

A constant default, or a stable one such as `now()`, adds the column without rewriting the table. A volatile default such as `gen_random_uuid()` rewrites it under lock: add the column without a default, set the default, then fill the existing rows in batches from `dml/`.

#### Changing a Column Type ↔️

Some changes only update the catalog: widening a `VARCHAR(n)`, `VARCHAR` to `TEXT`, and `TIMESTAMP` to `TIMESTAMP WITH TIME ZONE` under `SET TimeZone = 'UTC'`. Squawk flags them all the same: tolerate its rule with that reason.

```sql
-- Under UTC this converts the catalog only, and running it again on TIMESTAMP WITH TIME ZONE changes nothing
-- squawk-ignore changing-column-type, prefer-robust-stmts
ALTER TABLE drinkit_application.tasting ALTER COLUMN tasted TYPE TIMESTAMP WITH TIME ZONE;
```

In most other cases, changing a column's type rewrites the entire table. A safer approach is as follows:
1.  Create a **new column** with the desired type.
2.  Use a **trigger** to copy and synchronize data from the old column to the new one during `INSERT` and `UPDATE` operations. Its function needs a `$$` body that Liquibase splits wrongly: plan the first one with the maintainer.
3.  Update the **application code** to use the new column.
4.  Once the code is deployed and stable, drop the old column and the trigger in a later file.

#### Renaming and Dropping ✂️

During a deployment, the previous version of the application keeps running until the new one replaces it, and it still uses the old names. A rename breaks it at once. Change names in steps, which is called expand and contract:

1.  **Expand**: add the new column or table, and write to both.
2.  **Move**: copy the existing data in a `dml/` file, then switch the code to the new name.
3.  **Contract**: once no running version uses the old name, drop it in a later `ddl/` file.

A drop destroys data that no rollback brings back. Squawk refuses every drop until a `-- squawk-ignore` says, on the line above, why the data is no longer needed.

#### Changing Data 📦

- **Idempotent**: write `INSERT ... ON CONFLICT DO NOTHING`, or an `UPDATE` whose `WHERE` skips the rows already done, so that running it again changes nothing.
- **In batches**: an `UPDATE` of many rows holds the lock of each row until it ends, and leaves as many dead rows to clean. Split a large backfill in several statements of a few thousand rows each.
- **After the schema**: `dml/` runs after `ddl/`, so a backfill can fill a column added in the same deployment.

::: tip
More best practices [here](https://medium.com/paypal-tech/postgresql-at-scale-database-schema-changes-without-downtime-20d3749ed680)
:::

## ✔️ Checked Rules

`./gradlew :updater:check`, part of `./gradlew build`, runs two checks, and fails on any finding. It needs Node, at the version of `.nvmrc`, on the `PATH`.

- `ChangelogConventionsTest` reads every changelog with Liquibase's own parser, and checks how the folders and changesets are written.
- [Squawk](https://squawkhq.com/docs/rules), a linter for PostgreSQL migrations, reads their SQL. `deployment/updater/.squawk.toml` lists every one of its rules, on or off, so that nobody has to know its defaults.

In CI, the Updater lane runs the same command, then puts each Squawk finding on its line, in the "Files changed" tab of the pull request.

### What a Failure Looks Like

A rule of `ChangelogConventionsTest` names itself, says why it exists and how to fix it, then lists every file that breaks it:

```
ChangelogConventionsTest > every base migration only creates what is missing() FAILED
    Rule base-creates-only: base/ runs again on existing databases, where each statement must change nothing
    and block no read or write. Fix: keep to `SET`, `CREATE ... IF NOT EXISTS`, `CREATE INDEX CONCURRENTLY IF NOT EXISTS`
    and `COMMENT ON`, and write any other change in a ddl/ file. See "Checked Rules" in database.md.
    expected:<[]> but was:<["base/02-cellar-rooms.sql"]>
```

Squawk shows the rule with a link to its page, the statement, and the fix it suggests. Gradle then says what to do next:

```
warning[prefer-timestamp-tz]: When Postgres stores a datetime in a `timestamp` field, Postgres drops the UTC offset.
  ╭▸ src/main/resources/db/changelog/ddl/0001-create-tasting.sql:7:73
7 │ CREATE TABLE drinkit_application.tasting (id BIGINT PRIMARY KEY, tasted TIMESTAMP NOT NULL);
  ├ help: Use `timestamptz` instead of `timestamp` for your column type.
Found 2 issues in 1 file (checked 4 source files)

Execution failed for task ':updater:squawk'.
> Squawk found unsafe SQL in the changelogs above. Each finding names its rule, links to its page and shows a fix.
```

### Tolerating a Rule

A rule that does not apply to one statement is tolerated in its file, never for the whole project: `-- squawk-ignore <rule>` for the next statement, `-- squawk-ignore-file <rule>` for the whole file. The directive stands on a line of its own, right below a comment that says why the rule does not apply, as the examples of [Foreign Keys](#foreign-keys) show.

### Rules of `ChangelogConventionsTest`

| Rule | What it asks | Why |
|---|---|---|
| `formatted-sql-header` | The first line is `--liquibase formatted sql` | Without it, Liquibase runs the file as plain SQL and ignores its options |
| `one-changeset-per-file` | One changeset, whose id is the file name without `.sql` | The file and the run Liquibase records name each other |
| `fixed-logical-file-path` | `logicalFilePath:fixed` | A moved file would run again |
| `no-transaction` | `runInTransaction:false` | Each statement holds its locks alone, and `CONCURRENTLY` can run |
| `split-statements` | No `splitStatements:false` | Liquibase and Squawk see the same statements |
| `no-do-block` | No `DO` block | Squawk does not read the statements inside one |
| `timeouts-first` | `SET lock_timeout` and `SET statement_timeout` before any other statement | A statement that waits for its lock fails instead of blocking the queries behind it |
| `squawk-ignore-reason` | Each Squawk directive on a line of its own, right below a `-- <reason>` comment | Every tolerated violation says why. Squawk reads a directive in any comment, so a line that mentions `squawk-` counts as one |
| `known-folder` | Every file directly in `base/`, `ddl/` or `dml/` | Liquibase runs the folders in that order, and orders a subfolder by its whole path |
| `base-runs-on-change` | `runOnChange:true` in `base/` | `base/` runs again when it is edited or rebaselined |
| `base-creates-only` | Only `SET`, `CREATE ... IF NOT EXISTS`, `CREATE INDEX CONCURRENTLY IF NOT EXISTS` and `COMMENT ON` in `base/` | `base/` runs again on existing databases, where it must change nothing and block no read or write |
| `base-file-name` | A name such as `01-initial-schema.sql` | The level orders the files of `base/` |
| `run-once` | No `runOnChange` in `ddl/` and `dml/` | Running a change again takes its locks again |
| `numbered-file-name` | A name such as `0001-add-cellar-description.sql` | The number orders the files |
| `unique-file-number` | No two files of a folder share a number | Two files with one number run in an order nobody chose |
| `drop-index-before-build` | In `ddl/`, `DROP INDEX CONCURRENTLY IF EXISTS` right before each `CREATE INDEX CONCURRENTLY` of the same index | A failed build is rebuilt instead of kept as an invalid index |
| `ddl-changes-no-data` | No `INSERT`, `UPDATE`, `DELETE`, `MERGE`, `TRUNCATE` or `COPY` in `ddl/` | Data changes run after the schema, and are reviewed on their own |
| `dml-changes-no-schema` | Only `SET`, `RESET`, `INSERT`, `UPDATE`, `DELETE`, `MERGE`, `WITH` and `SELECT` in `dml/` | A schema change in `dml/` would run after the `ddl/` files and escape their rules |

### Rules of Squawk

`.squawk.toml` groups the rules by what they protect. Each rule has a page at `https://squawkhq.com/docs/<rule>`, with examples and the safe way to write the statement.

- **No long lock, no unbounded wait.** An index built or dropped without `CONCURRENTLY` blocks writes for the whole build, and a lock without a timeout can stall the application: `require-lock-timeout`, `require-statement-timeout`, `require-concurrent-index-creation`, `require-concurrent-index-deletion`, `require-concurrent-reindex`, `require-concurrent-partition-detach`, `ban-concurrent-index-creation-in-transaction`, `prefer-repack`, `transaction-nesting`, `ban-uncommitted-transaction`.
- **Runs again after a failure.** A file that failed halfway runs again from its first statement: `prefer-robust-stmts` asks for `IF NOT EXISTS` and `IF EXISTS`.
- **No scan or rewrite of a whole table under lock.** A constraint, a required column, a volatile default or a new type checks or rewrites every row while the table is locked: `constraint-missing-not-valid`, `adding-foreign-key-constraint`, `disallowed-unique-constraint`, `adding-not-nullable-field`, `adding-required-field`, `adding-field-with-default`, `adding-serial-primary-key-field`, `changing-column-type`, `ban-create-domain-with-constraint`, `ban-alter-domain-with-add-constraint`.
- **Types and names that do not run out, drift or surprise**, as the best practices above explain: `require-table-schema`, `prefer-timestamp-tz`, `prefer-bigint-over-int`, `prefer-bigint-over-smallint`, `prefer-identity`, `ban-char-field`, `identifier-too-long`, `require-enum-value-ordering`, `ban-add-enum-value`, `ban-add-composite-attribute`.
- **The running version keeps working.** A rename, a dropped default, a changed sequence or a replaced view changes what the previous version reads or writes: `renaming-column`, `renaming-table`, `renaming-object`, `ban-set-schema`, `ban-drop-not-null`, `ban-drop-default`, `ban-set-default`, `ban-alter-identity`, `ban-alter-sequence-values`, `ban-alter-generated-expression`, `ban-drop-generated-expression`, `ban-duplicate-column-assignments`, `ban-detach-inheritance`, `ban-replace-view-function`, `ban-alter-view-options`, `ban-alter-function-options`, `ban-disable-trigger`, `ban-drop-trigger`.
- **Nothing disappears without a written decision.** A drop destroys data or an object for good: `ban-drop-column`, `ban-drop-table`, `ban-drop-schema`, `ban-drop-database`, `ban-drop-sequence`, `ban-drop-type`, `ban-drop-domain`, `ban-drop-view`, `ban-drop-function`, `ban-drop-extension`, `ban-drop-constraint`, `ban-truncate-cascade`.
- **Access and server settings stay with operations.** A migration runs with elevated privileges, so it must not change roles, privileges, replication or server settings: `ban-alter-database-options`, `ban-alter-system-options`, `ban-alter-role-options`, `ban-alter-extension`, `ban-revoke`, `ban-replica-identity`, `ban-alter-row-level-security`, `ban-create-policy`, `ban-drop-policy`, `ban-alter-policy-condition`, `ban-alter-policy-roles`.

Four rules are off, each for a reason written in `.squawk.toml`:

- **`prefer-text-field`.** A `VARCHAR(n)` length is a deliberate guard, often the only one, and widening it changes the catalog only.
- **`ban-drop-index`.** A `ddl/` file drops each index right before it builds it.
- **`ban-add-column`.** Adding a nullable column is the first step of every expand and contract, and jOOQ names its columns, so a new one breaks no query.
- **`ban-new-write-restriction`.** Adding a constraint is the purpose of most `ddl/` files. The rules above check how it locks, and the expand and contract steps how the running version keeps working.

On a Squawk upgrade, read its changelog and put each new rule in one of the two lists of `.squawk.toml`: a misspelled or unknown name makes Squawk fail.

### Not Checked Yet

Neither check starts a database, so a few rules wait for a test against PostgreSQL ([#502](https://github.com/mboisnard/drinkit/issues/502)): a primary key on each table, an index behind each foreign key, no invalid index left behind, names without hyphens, the same schema for a new and an existing database after a rebaseline, and no read or write blocked when `base/` runs again.

## 📚 Sources

- [PostgreSQL at Scale: Database Schema Changes Without Downtime](https://medium.com/paypal-tech/postgresql-at-scale-database-schema-changes-without-downtime-20d3749ed680), PayPal
- [Explicit Locking](https://www.postgresql.org/docs/current/explicit-locking.html), the lock modes and what each one blocks, PostgreSQL documentation
- [Don't Do This](https://wiki.postgresql.org/wiki/Don%27t_Do_This), the types and habits to avoid, PostgreSQL wiki
- [Migration Style Guide](https://docs.gitlab.com/development/migration_style_guide/) and [Migration Squashing](https://docs.gitlab.com/development/database/migration_squashing/), GitLab
- [Squawk rules](https://squawkhq.com/docs/rules)
- [Best Practices](https://docs.liquibase.com/concepts/bestpractices.html) and [Trimming ChangeLogs](https://www.liquibase.com/blog/trimming-liquibase-changelogs), Liquibase
- [Baseline Migrations](https://documentation.red-gate.com/fd/baseline-migrations-273973336.html), Flyway

## ✨ jOOQ Code Generation

jOOQ generates Java/Kotlin classes from your database schema. This provides a type-safe DSL (Domain Specific Language) for writing elegant and safe SQL queries directly in your code.

### Why Commit Generated Code? 🚀

We commit the jOOQ-generated files to our code repository. The main reason is to **simplify the Continuous Integration (CI) pipeline**. Without this, the CI would have to:
1.  Start a PostgreSQL container using TestContainer.
2.  Run all Liquibase migrations.
3.  Generate the jOOQ code from this newly created database.
4.  Compile and test the application.

Committing the code avoids this overhead and complexity in the CI process. After a changelog change, apply it locally and regenerate the classes as the "Database and jOOQ" section of `AGENTS.md` describes.

## 🧪 Integration Testing

To ensure our queries work as expected, we use integration tests that connect to a real database.

-   **TestContainers** 🐳: This tool starts a PostgreSQL Docker container for each test suite. The database connection is then configured to point to this container.
-   **Using jOOQ Schemas**: Instead of using annotations like `@Sql` to load test data from SQL files, we can directly use the jOOQ-generated schemas to insert the necessary data for the test.

Here is a configuration example with a custom annotation in Kotlin:
```kotlin
// Usage in a test class
@JooqIntegrationTest(schemas = [DrinkitApplication::class])
class MyJooqRepositoryTest {
    // ... tests ...
}
```
