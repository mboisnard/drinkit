# Template of an ADR

Copy the block below into `docs/content/en/2.decisions/<number>-<title>.md`. Every line in angle brackets is replaced.
A section never stays empty: "Sources" goes when there are none.

```markdown
---
title: "<number>. <The decision, as a title>"
description: "<The summary below, in one sentence.>"
navigation:
  title: "<number> · <the title in four words at most, for the menu>"
status: <accepted, proposed, rejected, deprecated or superseded>
date: <YYYY-MM-DD, the day it was decided>
explainedIn:
  title: <the page that explains how DrinkIt applies the decision>
  to: </en/… route of that page>
supersedes: </en/decisions/… route of the ADR this one replaces, only when it replaces one>
supersededBy: </en/decisions/… route of the ADR that replaces this one, only once it is superseded>
---

<One or two sentences: what was decided, and the reason that weighed most.>

## Context

<The problem, the constraints and the facts that shaped the choice, for a reader who was not there.>

## Decision

<What the project does from now on, in the present tense, and what it does not do.>

## Consequences

- <What becomes easier, harder, slower or more expensive, one bullet each.>

## Alternatives

- **<Option>.** Rejected because <the reason>. It offered <what it offered>.

## Sources

- [<Title>](https://<address>), <publisher>
```
