---
name: new-adr
description: Records a structuring choice of DrinkIt as a short ADR that someone who was not there understands, and supersedes the ADR it replaces. Use it when asked for an ADR or a decision record, as soon as the maintainer settles a structuring choice in a session or an issue (a module, a dependency, a tool, the API contract, the schema layout, a security rule, a convention every contributor follows), and when a decision replaces an earlier one.
paths:
  - "docs/src/engineering/adr/**"
---

# Recording a decision

An ADR, architecture decision record, tells a future reader which choice the project made, why, and what it
costs. The guideline pages say how DrinkIt works, an ADR says why it works that way rather than another. ADRs live
in `docs/src/engineering/adr/`, and [its index](../../../docs/src/engineering/adr/index.md) lists them all. The
sections are in [template.md](template.md), and the prose follows the
[documentation-change](../documentation-change/SKILL.md) skill.

## 1. Check that it deserves an ADR

An ADR records a choice that:

- was made between at least two real options, the status quo counting as one
- costs a lot to reverse
- shapes the structure, a quality such as security or performance, a dependency, an interface or a tool that
  later contributors work with

The structuring choices of `AGENTS.md` usually qualify: a module, a dependency, the API contract, the schema
layout, a security rule. A detail inside a pattern already decided, a version bump or a bug fix does not.

One ADR holds one decision. A choice made of independent parts gets one ADR per part.

## 2. Make sure the maintainer decided

The maintainer makes the structuring choices. A choice settled in the session or the issue is recorded as
`accepted`. A choice still open is asked first, in the question format of `AGENTS.md`, or recorded as `proposed`
when the maintainer wants it written before deciding. An agent never records its own preference as a decision.

## 3. Gather the facts

- Take them from the issue, the pull request and the session: the problem, the constraints, the options compared
  and what each one offered. Leave out the story of the session.
- Check each fact against the code or the tool's documentation. A fact that ages carries its version or date, as
  ADR 0002 does with "measured on PostgreSQL 18.6".
- Keep the links of outside material for "Sources".

A decision already in force but never recorded is backfilled the same way. Its date is the day it was made: the
issue that settled it, or else the first commit that applied it.

## 4. Create the file

Take the next free number in `docs/src/engineering/adr/`, on four digits, never reused. Numbers follow the order
of writing, so a backfilled ADR can be older than the one before it. The file name is the number and the title in
kebab case. The title states the decision, such as "Architecture tests with Konture", not the question it
answered.

The `Issue` line links the issue where the maintainer decided. Other issues, such as the ones that carry out the
decision, are linked from the context.

## 5. Write each section

[template.md](template.md) gives the order.

- **Summary.** One or two sentences under the header: the decision and the reason that weighed most. A reader
  who stops there knows what was decided and why.
- **Context.** The problem and the forces at play, for a reader who was not there: what hurt, what constrained
  the choice, the facts measured. No option is argued here.
- **Decision.** What the project does from now on, in the present tense and in full sentences, and what it does
  not do.
- **How it is checked.** The architecture rule, Squawk rule, test or CI lane that fails when someone breaks the
  decision, by name. When nothing checks it yet, "Not checked", with the issue that would check it. When no
  such issue exists, create it with the `new-issue` skill.
- **Consequences.** What becomes easier, and what becomes harder, slower or more expensive, one bullet each, each
  one something a contributor will notice. A consequence may name what would reopen the decision.
- **Alternatives.** Each real option, with what it offered and why it lost, in one or two sentences.
- **Sources.** When outside material shaped the decision: the title as link text, then the publisher.

One or two screens are enough. The how-to and the detailed rules go to the guideline page, which links the ADR.

## 6. Avoid the known traps

Olaf Zimmermann names the ways an ADR fails its reader:

- **Fairy tale.** Gains only. Every decision costs something: write it down.
- **Sales pitch.** Marketing adjectives. Give the fact behind each one.
- **Free lunch coupon.** A cost left out because it is awkward, such as a Kotlin upgrade that now waits for a
  library release.
- **Dummy alternative.** An option nobody would pick, listed to make the choice look weighed.
- **Mega-ADR.** Several decisions, or a design document with its rollout plan. Split it, and move the design to a
  page.
- **Policy in disguise.** A list of rules without their reason. The rules go to the guideline page, the ADR keeps
  the why.

## 7. Supersede, never rewrite

A merged ADR keeps its text: only its status line changes, along with a typo or a dead link. When a decision
changes:

1. Write a new ADR whose header has a `Supersedes` line linking the old one, and whose context says what changed.
2. Set the status of the old one to `superseded by`, with a link to the new one.
3. Update both rows of the index.

A decision dropped with no replacement becomes `deprecated`, with the reason in the pull request. A `proposed`
ADR the maintainer turns down becomes `rejected` and stays, so that the question does not start again from
scratch.

## 8. Wire it

- Add its row to the table of the index, in the order of the numbers, and the page to the "Decision Records"
  group of the sidebar in `docs/.vitepress/config.ts`, as `<number>. <title>`, the title shortened when it runs
  long.
- Link it from the guideline page closest to the decision, with a sentence such as "ADR 0002 records why the
  changelogs are organized as below", as `database.md` does. That sentence follows the `documentation-change`
  skill, but it does not commit you to rewriting the rest of the page.
- The pull request that carries the decision carries its ADR.

## Before you finish

- [ ] The summary gives the decision and its main reason.
- [ ] Each alternative is real, and the consequences include at least one cost.
- [ ] "How it is checked" names a check, or says "Not checked" with an issue.
- [ ] The "Before you finish" list of the `documentation-change` skill passes for the ADR.
- [ ] The index, the sidebar and the guideline page link the ADR.

## Sources

- [Documenting Architecture Decisions](https://www.cognitect.com/blog/2011/11/15/documenting-architecture-decisions), Michael Nygard
- [MADR](https://adr.github.io/madr/), whose Confirmation section is "How it is checked" here
- [How to create ADRs, and how not to](https://ozimmer.ch/practices/2023/04/03/ADRCreation.html), Olaf Zimmermann
- [ADR process](https://docs.aws.amazon.com/prescriptive-guidance/latest/architectural-decision-records/adr-process.html), AWS Prescriptive Guidance
- [When should I write an Architecture Decision Record](https://engineering.atspotify.com/2020/04/when-should-i-write-an-architecture-decision-record), Spotify
