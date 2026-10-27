# Updater

The updater applies the database changes of DrinkIt to PostgreSQL, with Liquibase. It runs apart from the application, with the rights to change the schema, so that the application itself connects with a role that cannot.

## How It Works

`UpdaterApplication` is a Spring Boot application that runs Liquibase once, then stops. Liquibase reads `src/main/resources/db/changelog/db.changelog-master.yml`, which runs three folders, always in this order:

- **`base/`**: the schema, such as schemas, tables and indexes. A file runs again whenever it changes, and only creates what is missing.
- **`ddl/`**: the schema changes made after the base, one numbered file per change, run once.
- **`dml/`**: the data changes, one numbered file per change, run once, after every `ddl/` file.

Liquibase records each file it runs in `DRINKIT_DATABASECHANGELOG`, and skips it the next time.

## Run It

- **Apply the changelogs**: run `UpdaterApplication` from an IDE, against the local PostgreSQL of `deployment/local/compose.yml`. The psql command of the "Database and jOOQ" section of `AGENTS.md` does the same without the JVM.
- **Check the changelogs**: `./gradlew :updater:check` checks them against the conventions, with Liquibase's own parser and with [Squawk](https://squawkhq.com/docs/rules), whose binary Gradle downloads.

## Read Next

[`database.md`](../../docs/src/engineering/build-and-data/database.md) explains what goes in each folder, how to write a change that never blocks the users, how to rebaseline `base/`, and why each checked rule exists. [ADR 0002](../../docs/src/engineering/adr/0002-base-ddl-and-dml-changelogs-checked-by-squawk.md) records why the changelogs are organized this way.
