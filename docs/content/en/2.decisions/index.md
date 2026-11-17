---
title: "Architecture decision records"
navigation:
  title: About decision records
  icon: i-lucide-book-open
  tile: yellow
description: "When DrinkIt writes an ADR, and how to read and write one."
---

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

## How to write one

The [`new-adr` skill](https://github.com/mboisnard/drinkit/blob/master/.claude/skills/new-adr/SKILL.md) gives the
sections, the template and the checks, for people and coding agents alike.
