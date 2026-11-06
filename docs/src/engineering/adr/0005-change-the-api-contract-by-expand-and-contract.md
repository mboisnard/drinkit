# 0005. Change the API contract by expand and contract, frontend included

- Status: accepted
- Date: 2026-10-28

A change that breaks the API contract, such as a rename or a new required field, takes two pull requests, even when
the frontend and the backend change together. The first adds the new form next to the old one, the second removes
the old one once the first is deployed, because a browser tab opened before a deployment still uses the old client.

## Context

The [contract-first page](../architecture/contract-first.md) describes the API contract: one OpenAPI file, from which
the backend generates the interfaces it implements and the frontend the client it calls. Both live in one repository,
as [ADR 0000](0000-keep-every-project-in-one-repository.md) records, and are built from the same commit.

Built together is not deployed together. The frontend and the backend run as two processes, and a page loaded before a
deployment talks to the new backend until it reloads. A renamed field or a new required one then fails for that user,
and no check in CI sees it.

## Decision

A compatible change, such as an added operation, an added response field or a new optional request field, lands in
one pull request. A breaking change takes two, and the second waits until the first is deployed:

- **Replacing an element.** The first pull request adds the new field or operation, marks the old one deprecated and
  moves the frontend to the new one. The second removes the old one.
- **Adding a required request field.** The first pull request adds it as optional and makes the frontend send it.
  The second makes it required.

The frontend can move in the first pull request because its new code calls what the backend already serves, as long
as the backend deploys first. While DrinkIt is not deployed, the second pull request can follow the first as soon as
it is merged: the habit starts before anyone depends on it. The
[coding rules](../writing-code/best-practices/coding-rules.md#evolve-a-contract-in-steps) apply the same steps to the
other contracts, such as the database schema.

## Consequences

- A rename or a removal takes two pull requests and a deployment between them, where it took one.
- A deprecated element stays in the contract and the backend between the two, and someone has to come back to
  remove it.
- The backend has to deploy before the frontend.
- An open tab keeps working across a deployment, whatever the change.
- Until a CI check fails on a breaking change, the review of the contract diff applies the rule.

## Alternatives

- **A break in one pull request that adapts both sides.** Rejected because after each deployment that breaks, every
  user with an open tab gets errors until they reload. It offered one pull request per change.
- **One pull request until the first deployment, then expand and contract.** Rejected because the habit would start on
  a date that nothing marks. It offered no cost while nobody uses the application.

## Sources

- [Parallel Change](https://martinfowler.com/bliki/ParallelChange.html), Danilo Sato
- [Version Skew](https://www.industrialempathy.com/posts/version-skew/), Malte Ubl
- [Skew Protection](https://vercel.com/docs/skew-protection), Vercel, which keeps the old backend running instead of
  changing the contract in steps
