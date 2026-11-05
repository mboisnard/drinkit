# Architecture decision records

An architecture decision record, or ADR, explains one structuring choice of DrinkIt: the problem, the options
weighed, the one chosen and what it costs. The guideline pages describe how the project works. The ADRs keep why
it works that way, so that you can tell whether the reasons still hold before you change it.

## When we write one

We write an ADR when the maintainer chooses between real options and the choice costs a lot to reverse: a new
module or dependency, a tool, the API contract, the layout of the schema, a security rule, a convention every
contributor follows. A detail inside a pattern already decided, a version bump or a bug fix gets none. The pull
request that carries a decision carries its ADR.

## How to read one

An ADR gives its status, the context, the decision, its consequences and the alternatives that lost. One written
with the `new-adr` skill also opens with a summary of one or two sentences and says how the build checks the
decision.

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
| [0001](0001-architecture-tests-with-konture.md) | Architecture tests with Konture | Accepted | 2026-10-11 |
| [0002](0002-base-ddl-and-dml-changelogs-checked-by-squawk.md) | Base, DDL and DML changelogs, checked by Squawk | Accepted | 2026-10-24 |
| [0003](0003-api-contract-changes-by-expand-and-contract.md) | The API contract changes by expand and contract, frontend included | Superseded by 0015 | 2026-10-28 |
| [0004](0004-one-updater-image-for-every-environment.md) | One updater image for every environment | Accepted | 2026-10-29 |
| [0005](0005-demo-data-from-a-local-only-changelog.md) | Demo data from a local-only changelog | Accepted | 2026-10-29 |
| [0006](0006-mailpit-as-the-smtp-server-of-the-tests.md) | Mailpit as the SMTP server of the tests | Accepted | 2026-11-02 |
| [0007](0007-jooq-kotlin-maps-nested-multiset-reads.md) | jooq-kotlin maps nested MULTISET reads | Accepted | 2026-11-02 |
| [0008](0008-caching-through-spring-on-caffeine-with-a-ttl-per-cache.md) | Caching through Spring's abstraction, on Caffeine, with a time to live per cache | Accepted | 2026-11-03 |
| [0009](0009-search-index-writes-through-the-starter-searches-through-the-engine-sdk.md) | Search index writes through the starter, searches through the engine's SDK | Accepted | 2026-11-03 |
| [0010](0010-search-index-rebuilds-in-batches-tracked-in-postgresql.md) | Search index rebuilds in batches tracked in PostgreSQL | Accepted | 2026-11-04 |
| [0011](0011-a-module-declares-the-event-sourcing-starter-itself.md) | A module declares the event sourcing starter itself | Accepted | 2026-11-04 |
| [0012](0012-stateful-sessions-in-postgresql-serialized-as-jsonb.md) | Stateful sessions in PostgreSQL, serialized as JSONB | Accepted | 2026-11-05 |
| [0013](0013-remember-me-by-a-rotating-hashed-token.md) | Remember-me by a rotating, hashed token | Accepted | 2026-11-05 |
| [0014](0014-argon2id-for-passwords-bouncycastle-as-a-dependency.md) | Argon2id for passwords, BouncyCastle as a dependency | Accepted | 2026-11-05 |
| [0015](0015-the-api-contract-may-break-in-one-pull-request-until-the-first-deployment.md) | The API contract may break in one pull request until the first deployment | Accepted | 2026-11-05 |

## How to write one

The [`new-adr` skill](https://github.com/mboisnard/drinkit/blob/master/.claude/skills/new-adr/SKILL.md) gives the
sections, the template and the checks, for people and coding agents alike.
