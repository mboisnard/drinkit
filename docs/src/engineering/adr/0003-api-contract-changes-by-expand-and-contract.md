# 0003. The API contract changes by expand and contract, frontend included

- Status: accepted
- Date: 2026-10-28
- Issue: [#513](https://github.com/mboisnard/drinkit/issues/513)

A change that breaks the API contract, such as a rename or a new required field, takes two pull requests, even
when the frontend and the backend change together. The first adds the new form next to the old one, the second
removes the old form once the first is deployed, because a browser tab opened before a deployment still uses the
old client. The application is not deployed yet, so the rule starts before anyone depends on it.

## Context

The [contract-first page](../architecture/contract-first.md) describes the API contract: one OpenAPI file in the
`drinkit-api-contract` module, from which the backend generates the interfaces it implements and the frontend the
TypeScript client it calls. Until now, the [`api-contract-change` skill][skill], which coding agents follow to
change the contract, allowed a breaking change on one condition: the same pull request adapted the other side. Its
reason was that both sides are built from the same commit.

Built together is not deployed together. The Nuxt frontend and the Spring Boot backend run as two processes, and
Malte Ubl calls the result version skew: a page loaded before a deployment talks to the new backend until it
reloads. A renamed field or a new required one then fails for that user, and no check in CI sees it.

## Decision

A compatible change, such as an added operation, an added response field or a new optional request field, lands in
one pull request. A breaking change takes two, and the second waits until the first is deployed:

- **Replacing an element.** The first pull request adds the new field or operation, marks the old one
  `deprecated: true` and moves the frontend to the new one. The second removes the old one.
- **Adding a required request field.** The first pull request adds it as optional and makes the frontend send it.
  The second makes it required.

The frontend can move in the first pull request because its new code calls an element the backend already serves,
as long as the backend deploys before the frontend. Renaming an `operationId` or a tag stays one pull request:
only the generated names change, not the HTTP exchange.

While the application is not deployed, the second pull request can follow the first as soon as it is merged: the
two pull requests stay, only the wait goes. The rule also holds for any later caller of the API, such as a second
backend. The contract-first page lists which changes break, and the
[coding rules](../writing-code/best-practices/coding-rules.md#evolve-a-contract-in-steps) apply the same steps to
the other contracts, such as the database schema.

## How it is checked

Not checked. [#526](https://github.com/mboisnard/drinkit/issues/526) tracks a CI check that fails on a breaking
change of the contract, except the removal of an element that an earlier pull request marked deprecated. Until
then, the review of the contract diff applies the rule.

## Consequences

- A rename or a removal takes two pull requests and a deployment between them, where it took one.
- A deprecated element stays in the contract and in the backend between the two pull requests, and someone has to
  come back to remove it.
- The backend has to deploy before the frontend. Where that order cannot be guaranteed, the move of the frontend
  takes a pull request of its own, after the first one is deployed.
- An open tab keeps working across a deployment, whatever the change.

## Alternatives

- A break in one pull request that adapts both sides, the rule until now: one pull request per change, but after
  each deployment that breaks, every user with an open tab gets errors until a reload.
- A break in one pull request until the first deployment, then expand and contract: no cost while nobody uses the
  application, but the two-step habit would start on a date that nothing marks, and the contract-first page and
  the skill would change again.

## Sources

- [Version Skew](https://www.industrialempathy.com/posts/version-skew/), Malte Ubl
- [Parallel Change](https://martinfowler.com/bliki/ParallelChange.html), Danilo Sato

[skill]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/api-contract-change/SKILL.md
