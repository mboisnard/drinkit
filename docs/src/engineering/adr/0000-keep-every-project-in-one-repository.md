# 0000. Keep every project in one repository

- Status: accepted
- Date: 2026-10-24

Everything DrinkIt is made of lives in one repository: the frontend, the Kotlin backend, the tech starters, the build
logic, the database changelogs, the coding agents' skills, the documentation site, and later the machine learning code
and its evaluation tools. One clone shows the whole picture, a change and everything it affects travel in one pull
request, and a contract is shared without a registry.

## Context

DrinkIt is a cellar manager for wines, beers and spirits, and a sandbox for engineering practices. Its parts depend on
each other: the frontend calls the backend through an OpenAPI contract, the backend builds on the
[tech starters](../architecture/tech-starters.md), the documentation describes all of them, and the coding agents
follow skills that name their files. One person works on it, with coding agents, and both need to see how a change
in one part reaches the others.

Split into one repository per part, each change to a shared piece would need a release, a version bump in each
consumer and a pull request per repository, and a contract would need a registry to be shared.

## Decision

DrinkIt keeps every project in one repository, laid out as the [monorepo page](../architecture/monorepo.md) describes:

- **The big picture in one place.** A newcomer, human or agent, finds the code, its contracts, its conventions, its
  deployment and its documentation in one clone, with nothing to discover elsewhere.
- **No internal release.** A shared piece, such as a tech starter, is used from its folder. There is one version of
  everything, the commit, so packaging and versioning stay simple.
- **Contracts shared from the repository.** The OpenAPI contract of the API lives in a module that the backend and
  the frontend both generate code from. An asynchronous contract, such as an AsyncAPI file for the
  [platform events](../architecture/platform-events.md), would be shared the same way, with no schema registry.
- **Projects stay apart.** One repository is not one project: an application never uses the modules of another, and
  a tech starter never uses an application, as the [architecture tests](0004-check-the-architecture-with-tests.md)
  check.

## Consequences

- A change to a shared piece updates its users in the same commit, and the build shows at once what it breaks.
- The build conventions, the code analysis, the CI and the dependency updates apply to every project at once.
- The documentation changes in the same pull request as the code it describes.
- A change can reach everything, so the CI has to run only what a change touches, through lanes that watch paths,
  and caches, to stay fast.
- Built together does not mean deployed together: the frontend and the backend still deploy apart, which is why the
  API contract changes in two steps, as [ADR 0005](0005-change-the-api-contract-by-expand-and-contract.md) records.
- With several teams, a change across projects would need their agreement before the merge. A repository far larger
  than DrinkIt's would need dedicated build and version control tools.

## Alternatives

- **One repository per project.** Rejected because every shared change would need a release and a bump in each
  consumer, and the API contract a registry such as Apicurio to be shared. It offered smaller repositories, each owned
  apart.
- **The code in one repository, the documentation and the skills in others.** Rejected because the documentation and
  the agents' skills drift from the code they describe as soon as they change in another pull request. It offered a
  smaller code repository.

## Sources

- [Monorepo Explained](https://monorepo.tools/), Nx
- [Monorepo: please do!](https://medium.com/@adamhjk/monorepo-please-do-3657e08a4b70), Adam Jacob
- [Monorepos: Please don't!](https://medium.com/@mattklein123/monorepos-please-dont-e9a279be011b), Matt Klein, the
  case against, from the tooling a large monorepo needs
