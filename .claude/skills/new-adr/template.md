# Template of an ADR

Copy the block below into `docs/src/engineering/adr/<number>-<title>.md`. Every line in angle brackets is replaced.
A section never stays empty: "How it is checked" says "Not checked" with its issue, and "Sources" goes when there
are none.

```markdown
# <number>. <The decision, as a title>

- Status: <accepted, proposed, rejected, deprecated, or superseded by a link to the new ADR>
- Date: <YYYY-MM-DD, the day it was decided>
- Issue: [#<number>](https://github.com/mboisnard/drinkit/issues/<number>)
- Supersedes: <a link to the ADR this one replaces, only when it replaces one>

<One or two sentences: what was decided, and the reason that weighed most.>

## Context

<The problem, the constraints and the facts that shaped the choice, for a reader who was not there.>

## Decision

<What the project does from now on, in the present tense, and what it does not do.>

## How it is checked

<The rule, test or CI lane that fails when the decision is broken, by name, or "Not checked" with its issue.>

## Consequences

- <What becomes easier, harder, slower or more expensive, one bullet each.>

## Alternatives

- <Option>: <what it offered, and why it lost.>

## Sources

- [<Title>](https://<address>), <publisher>
```
