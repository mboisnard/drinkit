---
title: "024. Generate the frontend's API client with Hey API, from the app's contract, at each build"
description: "A Nuxt app calls the backend only through a client that Hey API generates from its OpenAPI contract at each prepare and build, never committed, whose calls take the cookies of their own request."
navigation:
  title: "024 · API client by Hey API"
status: accepted
date: 2026-11-19
explainedIn:
  title: Calling the API
  to: /en/reference/security/calling-the-api
---

A Nuxt app calls the backend only through a TypeScript client that Hey API generates from the app's OpenAPI
contract, each time Nuxt prepares or builds the app. The client is never committed, so a change of the contract that
breaks a page fails the type check of the next build.

## Context

The backend generates its Spring interfaces from the contract ([contract first](../1.engineering/2.architecture/5.contract-first.md)),
and the frontend needed the same guarantee. Several apps will each have their own contract, so the generation is
shared by the `api-client-starter` while each app points it at its own file.

A server render calls the backend from Nitro, which serves the requests of every user. A client that holds the
cookies of one request in a shared object would hand them to another user's call. The calls of a page also need
the CSRF header and the problem detail of a refusal, which a generated client knows nothing of.

Hey API 0.99.0, measured in November 2026, is still at 0.x: a minor version may break, as 0.97 did with fifteen
changes.

## Decision

**Hey API generates the client.** Its `@hey-api/openapi-ts` package is pinned to an exact version in the catalog. It
generates the types and one function per operation, with its own ofetch client copied into the output, so the app
installs no client package beyond `ofetch`.

**The starter generates it, the app names its contract.** The app sets in its `nuxt.config.ts` the contract, the
path of its API and the local origin of its backend, under `apiClient`, so each app reaches its own backend.
The module of the starter writes the client under `.nuxt/openapi` at each prepare, dev and build, and the app imports
it as `#api`. No generated file is committed, and the frontend lane of CI runs when the contract changes.

**Each call takes the options of its own request.** A page passes `useApi()` to every function of the client. In the
browser it points at the same-origin relay of
[ADR 023](./023-render-the-frontend-with-nuxt-on-tech-starters-in-a-pnpm-workspace.md). In a server render it points at the backend and carries the
cookies of that request. The shared client of the generated code fails at once when a call omits them. A refused
call throws a `ProblemError`.

The client of Hey API for Nuxt, marked beta, is not used.

## Consequences

- A contract change that breaks a page fails `nuxt typecheck`, in the pull request that changes the contract.
- An app gets its client with one line of configuration, and a new contract needs no script.
- Every call repeats `useApi()`, which the shared client checks, rather than a configuration set once.
- Each bump of Hey API comes with its release notes and may need a change of the module or the starter.
- A page calls `useAsyncData` itself: the client does not wrap the composables of Nuxt.

## Alternatives

- **orval.** Rejected in favor of the generator most used, about twice as many weekly downloads in November 2026,
  whose plugins cover validation and transformers should DrinkIt need them. It offered a stable major version and
  a mutator, a function of our own that the generated code calls.
- **OpenAPI Generator, `typescript-fetch`.** Rejected because it needs Java in the frontend build and Mustache
  templates to bend its runtime to Nuxt. It offered the generator the backend already uses.
- **openapi-typescript with openapi-fetch.** Rejected because openapi-fetch went into maintenance mode in December
  2025. It offered types only and a client of a few kilobytes.
- **The Nuxt client of Hey API.** Rejected because it is beta, imports Nuxt so that a Nitro route cannot use it, and
  still needs our options for the cookies. It offered `useFetch` and `useAsyncData` behind one option of the call.

## Sources

- [Get started](https://heyapi.dev/openapi-ts/get-started), Hey API
- [ofetch client](https://heyapi.dev/openapi-ts/clients/ofetch), Hey API
- [Roadmap of openapi-typescript](https://github.com/openapi-ts/openapi-typescript/discussions/2559), openapi-ts, on the maintenance of openapi-fetch
