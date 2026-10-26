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

## How to write one

The [`new-adr` skill](https://github.com/mboisnard/drinkit/blob/master/.claude/skills/new-adr/SKILL.md) gives the
sections, the template and the checks, for people and coding agents alike.
