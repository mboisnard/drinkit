# Updater

The updater applies the database changes of DrinkIt to PostgreSQL, with Liquibase. It runs apart from the application, with the rights to change the schema, so that the application itself connects with a role that cannot.

## How It Works

`UpdaterApplication` is a Spring Boot application that runs Liquibase once, then stops. Liquibase reads `src/main/resources/db/changelog/db.changelog-master.yml`, which runs three folders, always in this order:

- **`base/`**: the schema, such as schemas, tables and indexes. A file runs again whenever it changes, and only creates what is missing.
- **`ddl/`**: the schema changes made after the base, one numbered file per change, run once.
- **`dml/`**: the data changes, one numbered file per change, run once, after every `ddl/` file.

Liquibase records each file it runs in `DRINKIT_DATABASECHANGELOG`, and skips it the next time.

The `Dockerfile` of this folder builds it into an image that holds no changelog: it reads them from the classpath under `/workspace`, so `db/` mounted at `/workspace/db` gives it the changelogs of your checkout.

## Run It

- **Apply the changelogs**: run `UpdaterApplication` from an IDE with the `dev` profile, whose `application-dev.yml` points at the local PostgreSQL of `deployment/local/compose.yml`. Without a profile, the updater has no database to connect to and stops. The psql command of the `database-change` skill, `.claude/skills/database-change/SKILL.md`, does the same without the JVM.
- **Run the image**: `./gradlew :updater:extractLayers`, then `docker build --file deployment/updater/Dockerfile --tag drinkit-updater deployment/updater/build/image` from the root. Run it with `--volume "$PWD/deployment/updater/src/main/resources/db:/workspace/db:ro"`, plus `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD`: the image holds no default credentials. Against the PostgreSQL of compose, that is `--network local_default` and `jdbc:postgresql://postgresql:5432/drinkit`, with the `drinkit` role.
- **Check the changelogs**: `./gradlew :updater:check` checks them against the conventions, with Liquibase's own parser and with [Squawk](https://squawkhq.com/docs/rules), whose binary Gradle downloads.

## Read Next

[`database.md`](../../docs/src/engineering/build-and-data/database.md) explains what goes in each folder, how to write a change that never blocks the users, how to rebaseline `base/`, and why each checked rule exists. [ADR 0002](../../docs/src/engineering/adr/0002-base-ddl-and-dml-changelogs-checked-by-squawk.md) records why the changelogs are organized this way.
