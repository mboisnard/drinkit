# 0005. Demo data from a local-only changelog

- Status: accepted
- Date: 2026-10-29
- Issue: [#403](https://github.com/mboisnard/drinkit/issues/403)

A local start gets its demo data from a changelog that only compose runs, after the changelog of every environment.
No deployed database can receive the demo admin, whose password is in the repository, since no deployment runs
that changelog.

## Context

A newcomer should log in and list a cellar right after the first start. Logging in needs a user row with a bcrypt
password. That row is a projection: the user's history in `user_event` rebuilds it for every command, so the demo
user needs its events too. The API cannot create the whole account: registration waits for an emailed token, no
operation grants the admin role, and creating a cellar fails until #389. The demo account is an admin, so that the
OpenAPI page, which requires that role, opens. With its password public, it must never reach a deployed database.

## Decision

The demo data sits in the updater, beside the changelogs, in `src/main/resources/db/demo/`. Compose runs its
`db.changelog-demo.yml`, which includes the changelog of every environment, then the SQL files of that folder.
Those files insert the rows that the API would write: the events of `demo@drinkit.test` from its registration to
its promotion as admin, its projection, and a cellar, with fixed ids and `ON CONFLICT DO NOTHING`. The demo
changesets carry no Liquibase context or label: deployments run the changelog of every environment, which does not
include them. No demo bottle exists until a table stores bottles.

Since the password is public, [Developer experience](../writing-code/best-practices/dev-exp.md) lists it among the
local values, the page the security reviewer reads.

## How it is checked

The Ops lane starts compose on every change to `deployment/local` or to the updater's `db/` folder, so a demo row
that the schema refuses fails it. Squawk and `ChangelogConventionsTest` read `db/changelog/` only.

## Consequences

- A fresh clone reaches a logged-in admin with a cellar in one command.
- The demo rows copy the JSON that the backend writes for the user's events. A change to those payloads updates
  the demo files by hand: the Ops lane catches a row the schema refuses, not a payload the backend no longer reads.
- A demo file that changes runs again, but `ON CONFLICT DO NOTHING` keeps the rows already there: a changed row
  reaches a local database once it is recreated.
- All the SQL lives in the updater. The image that a deployment builds copies `db/changelog/`, not the whole `db/`,
  or it would carry the demo password, even though nothing there runs it.
- Whoever reaches a developer's port 8080, from the same network too, can log in as an admin of that local
  database.

## Alternatives

- A Liquibase context on the demo changesets, in the changelog of every environment: one changelog, but Liquibase
  runs every changeset when no context is given, so a deployment that forgets the filter would create the admin.
- Registration through the API, from a script run after the start: the rows would come from the code, but the
  emailed token, the admin role and the cellar have no operation that works today.
- A dump restored into the volume: the whole database at once, but every changelog change would mean a new dump.
- The demo files under `deployment/local`, mounted apart from the changelogs: nothing of the demo in the updater,
  but the SQL of the database in two places.
