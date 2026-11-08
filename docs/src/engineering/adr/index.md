# Architecture decision records

An architecture decision record, or ADR, explains one structuring choice of DrinkIt: the problem, the options
weighed, the one chosen and what it costs. The guideline pages describe how the project works. The ADRs keep why
it works that way, so that you can tell whether the reasons still hold before you change it.

## When we write one

We write an ADR when the maintainer chooses between real options and the choice costs a lot to reverse: a new
module or dependency, a tool, the API contract, the layout of the schema, a security rule, a convention every
contributor follows. A detail inside a pattern already decided, a choice of test tool, a dependency line, a version
bump or a bug fix gets none: the page or the README of what it concerns says it. The pull request that carries a
decision carries its ADR.

## How to read one

An ADR opens with a summary of one or two sentences, then gives the context, the decision, its consequences and the
alternatives that lost. It explains why, and links the page that explains how.

An accepted ADR is never rewritten. When a decision changes, a new ADR supersedes the old one, and each links to
the other. The status says where a decision stands:

- **Proposed.** Written, not decided yet.
- **Accepted.** In force.
- **Rejected.** Weighed and turned down, kept so that the question does not start again from scratch.
- **Deprecated.** No longer in force, with nothing in its place.
- **Superseded.** Replaced by the ADR its status links to.

## Decisions

| ADR | Decision | Status | Date |
|---|---|---|---|
| [0000](0000-keep-every-project-in-one-repository.md) | Keep every project in one repository | Accepted | 2026-10-24 |
| [0001](0001-build-with-gradle-and-shared-conventions.md) | Build with Gradle and shared conventions | Accepted | 2026-10-24 |
| [0002](0002-store-data-in-postgresql-and-query-it-with-jooq.md) | Store data in PostgreSQL, and query it with jOOQ | Accepted | 2026-10-24 |
| [0003](0003-apply-the-schema-through-the-updater.md) | Apply the schema with Liquibase, through the updater | Accepted | 2026-10-24 |
| [0004](0004-check-the-architecture-with-tests.md) | Check the architecture with tests | Accepted | 2026-10-25 |
| [0005](0005-change-the-api-contract-by-expand-and-contract.md) | Change the API contract by expand and contract, frontend included | Accepted | 2026-10-28 |
| [0006](0006-carry-platform-events-through-an-abstraction.md) | Carry platform events through an abstraction | Accepted | 2026-10-30 |
| [0007](0007-cache-through-spring-on-caffeine.md) | Cache through Spring's abstraction, on Caffeine | Accepted | 2026-11-03 |
| [0008](0008-index-through-an-abstraction-search-with-meilisearch.md) | Index through an abstraction, search with Meilisearch | Accepted | 2026-11-03 |
| [0009](0009-rebuild-search-indexes-in-batches-tracked-in-postgresql.md) | Rebuild search indexes in batches tracked in PostgreSQL | Accepted | 2026-11-04 |
| [0010](0010-send-emails-over-smtp-to-a-server-chosen-per-environment.md) | Send emails over SMTP to a server chosen per environment | Accepted | 2026-11-06 |
| [0011](0011-sign-in-without-a-password.md) | Sign in without a password | Accepted | 2026-11-06 |
| [0012](0012-keep-sessions-in-postgresql.md) | Keep sessions in PostgreSQL | Accepted | 2026-11-06 |
| [0013](0013-remember-me-with-a-rotating-hashed-token.md) | Remember me with a rotating, hashed token | Accepted | 2026-11-06 |
| [0014](0014-protect-against-csrf-with-a-cookie-token-and-fetch-metadata.md) | Protect against CSRF with a token in a cookie and Fetch Metadata | Accepted | 2026-11-06 |
| [0015](0015-push-server-events-over-sse-to-a-user-or-a-session.md) | Push server events over SSE, to a user or a session | Accepted | 2026-11-07 |
| [0016](0016-observe-through-micrometer-and-export-over-opentelemetry-on-demand.md) | Observe through Micrometer, and export over OpenTelemetry on demand | Accepted | 2026-11-07 |
| [0017](0017-feature-flags-through-openfeature.md) | Feature flags through OpenFeature, with Flipt as provider | Accepted | 2026-11-08 |
| [0018](0018-ab-tests-behind-a-flag-with-posthog.md) | A/B tests behind a flag, with PostHog choosing the variant | Accepted | 2026-11-08 |

## How to write one

The [`new-adr` skill](https://github.com/mboisnard/drinkit/blob/master/.claude/skills/new-adr/SKILL.md) gives the
sections, the template and the checks, for people and coding agents alike.
