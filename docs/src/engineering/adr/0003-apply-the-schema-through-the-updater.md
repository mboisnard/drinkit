# 0003. Apply the schema with Liquibase, through the updater

- Status: accepted
- Date: 2026-10-24

The schema changes through Liquibase changelogs that a dedicated application, the updater, applies to PostgreSQL. The
backend never changes the schema. The updater ships as one image that every environment runs with its own
changelogs, and the demo data of a local start comes from a changelog that no deployment runs.

## Context

The backend's role in production reads and writes data, but cannot change the schema: a flaw in the backend then
cannot drop a table. Something else has to apply the changelogs, before the backend of the same commit starts, and
the maintainer wants that program to grow into the one that maintains the infrastructure: the schema today, message
queues and feature flags later.

A migration also runs while users are still using the application, so a change must never hold a lock that blocks
their reads and writes, and a newcomer should reach a signed-in account with a cellar right after the first start.

## Decision

- **A dedicated updater.** The updater is a Spring Boot application that runs Liquibase once, then stops. Its image,
  published on GHCR, holds no changelog, so a new changelog never needs a new image. A local start mounts the
  changelogs from the repository and runs the updater at each start of the backend. A deployment will copy them into
  an image built from this one, so that its digest names the exact SQL.
- **Changelogs organized for safe migrations.** `base/` shows the whole schema and only creates what is missing, so
  running it again changes nothing and blocks nobody. `ddl/` holds the later schema changes and `dml/` the data
  changes, one numbered file per change, run once. Squawk, a PostgreSQL migration linter, and a convention test check
  every file in the build, so a statement that would lock a table fails before it is merged.
- **Demo data that stays local.** The demo account and its cellar come from a `demo/` changelog that only the local
  compose runs, after the changelog of every environment. Its password is public, so no deployment includes it.

The [database page](../build-and-data/database.md) gives the rules of each folder and how to write a change, and
[container images](../delivery/container-images.md) how the image is built and published.

## Consequences

- Every environment runs the same updater, built and tested once by CI, and no developer builds it.
- A pulled changelog applies at the next start of the backend, which takes about 2 seconds more.
- A `ddl/` or `dml/` file cannot change once it has run: a fix is the next file. A schema change that needs data
  first waits for the next deployment, since `dml/` runs after `ddl/`.
- Squawk's GitHub releases become a source of the build, next to Maven Central.
- A change to the updater's own code reaches compose once its image is published. Until then, it runs from the IDE.
- The demo rows copy the JSON the backend writes for the user's events, and are updated by hand when it changes.

## Alternatives

- **Liquibase inside the backend.** Rejected because the backend would need the right to change the schema, and the
  updater could no longer grow to queues and feature flags. It offered no image to publish.
- **The official Liquibase image.** Rejected because it runs none of DrinkIt's code, and no longer ships the
  PostgreSQL driver. It offered an image maintained by someone else.
- **One numbered file per change, `base/` included.** Rejected because no file would show the whole schema any more.
  It offered Squawk a view of every statement.
- **Demo data behind a Liquibase context.** Rejected because a deployment that forgets the filter creates the demo
  admin, whose password is public. It offered one changelog.

## Sources

- [Evolutionary Database Design](https://martinfowler.com/articles/evodb.html), Pramod Sadalage and Martin Fowler
- [Zero-downtime Postgres migrations, the hard parts](https://gocardless.com/blog/zero-downtime-postgres-migrations-the-hard-parts/), GoCardless
- [Squawk](https://squawkhq.com/), a linter for PostgreSQL migrations
