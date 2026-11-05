# 0015. The API contract may break in one pull request until the first deployment

- Status: accepted
- Date: 2026-11-05
- Issue: [#590](https://github.com/mboisnard/drinkit/issues/590)
- Supersedes: [ADR 0003](0003-api-contract-changes-by-expand-and-contract.md)

Until DrinkIt is deployed for the first time, a change that breaks the API contract lands in one pull request that
adapts both sides. From the first deployment on, it takes the two pull requests of expand and contract, as ADR 0003
decided: splitting a change buys nothing while no browser tab and no other backend calls the API.

## Context

ADR 0003 started expand and contract before anyone depended on the API, so that the habit would be in place on the
day of the first deployment. Since then, the foundations of the API are being laid before any business feature
uses them: the security rework of #590 changed the login answer from 200 to 204, bounded the login and registration
fields and added a role to a response, in a contract that no client calls yet. Splitting each such change into two
pull requests doubles the reviews of a contract still taking shape, and protects no one.

## Decision

- While nothing is deployed, a breaking change of the contract, such as a new bound, a new response value, a status
  change or a removal, lands in one pull request that changes the contract, the backend and the frontend together.
  The pull request says which changes break.
- The first deployment ends this exception. From then on, every breaking change takes an expand and a contract, each
  in its own pull request, in the order the contract-first page gives, for the frontend as for any other caller.
- Renaming an `operationId` or a tag stays one pull request at any time: it changes no HTTP exchange.

## How it is checked

Not checked. [#526](https://github.com/mboisnard/drinkit/issues/526) tracks a CI check that fails on a breaking
change of the contract, and goes live with the first deployment. Until then, the review of the contract diff applies
the rule.

## Consequences

- A breaking change before the first deployment takes one pull request and one review.
- Expand and contract starts on the day of the first deployment, which the pull request that deploys must mark by
  turning on the check of #526.
- A contributor reading an older pull request sees breaking changes in one step, and finds the reason here.

## Alternatives

- Keep ADR 0003 as it is: two pull requests from now on, a cost paid for callers that do not exist yet.
- Drop expand and contract for good: one pull request per change, but every deployment that breaks would fail the
  open tabs until a reload.

## Sources

- [Parallel Change](https://martinfowler.com/bliki/ParallelChange.html), Danilo Sato
