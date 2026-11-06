# Developer experience

Two commands start DrinkIt on a developer's machine: one for the backend, one for the frontend. The backend starts
the containers it needs and fills the database when it has to, so a fresh clone needs no other step. Read this
page before you change how DrinkIt starts locally.

## 🚀 Two commands

The [Commands section of `AGENTS.md`](https://github.com/mboisnard/drinkit/blob/master/AGENTS.md#commands) gives
both, and what the frontend needs before its first start. The backend serves the API on `localhost:8080`, the
frontend the app on `localhost:3000`.

- **The database fills itself.** Each start of the backend runs the
  [updater](../../build-and-data/database.md#liquibase) through compose, which applies the
  [changelogs](../../build-and-data/database.md#folders) that the database has not received yet, then the demo
  data. A new database gets its schema, and a changelog you pull reaches yours at the next start. A changelog that
  fails stops the backend.
- **An account is ready to sign in.** `demo@drinkit.test` is an admin of the local database. It signs in with a
  code that the backend emails to [Mailpit](https://mailpit.axllent.org/), which keeps every email of a local start
  and shows it on `http://localhost:8025`: nothing reaches a real mailbox.
  [ADR 0010](../../adr/0010-send-emails-over-smtp-to-a-server-chosen-per-environment.md) records how each
  environment sends its emails.
  [ADR 0003](../../adr/0003-apply-the-schema-through-the-updater.md) records why the demo data never reaches a
  deployed database.

Every checkout of the repository, a worktree too, starts the same compose project, `local`, named after the folder
of `deployment/local/compose.yml`. The checkouts share its database: by default, a worktree's start applies the
changelogs of its unmerged branch there. The
[traps of `AGENTS.md`](https://github.com/mboisnard/drinkit/blob/master/AGENTS.md#traps) say how a worktree runs a
project of its own. Stop that project once the work in the worktree ends: its containers hold the ports of `local`.

## 🩹 When the updater stops the start

The backend waits for the updater, so the start fails with it, and
`docker compose -f deployment/local/compose.yml logs updater` shows why. Some failures come from the local
database, not from a changelog, and a statement run through
`docker compose -f deployment/local/compose.yml exec postgresql psql -U drinkit -d drinkit -c "<statement>"` fixes
each:

- **A changelog edited after it ran.** Liquibase logs "Validation Failed: 1 changesets check sum" and stops, since
  a file that ran never changes, as [Folders](../../build-and-data/database.md#folders) explains. While the file is
  yours and unmerged, so that no other database has run it, delete its row and it runs again at the next start:
  `DELETE FROM drinkit_databasechangelog WHERE id = '<file name without .sql>'`. Its statements only create what
  is missing, so an object its first version created keeps that shape: a new database, as below, gets the edit.
- **A lock left by a killed updater.** Stopping a start while the updater applies a changelog leaves Liquibase's
  lock taken. The next updater with a changelog to apply waits for it, logging "Waiting for changelog lock....",
  and the start fails once compose stops waiting, after 2 minutes. Release the lock:
  `UPDATE drinkit_databasechangeloglock SET locked = FALSE, lockgranted = NULL, lockedby = NULL`.

Either failure also goes away with a new database: `docker compose -f deployment/local/compose.yml down -v`
deletes the volumes of the local services, Meilisearch's and Flipt's too, and the next start rebuilds the schema
and the demo data. In a worktree, give each of these commands the `COMPOSE_PROJECT_NAME` of its start, or they read
and change the shared database.

## 🔓 Values public on purpose

These values are in the repository because they only exist locally. Any other credential comes from an environment
variable:

- the `drinkit` role and its password `admin`, and the Meilisearch key `masterKey`, in `deployment/local/compose.yml`
  and the `application-dev.yml` files
