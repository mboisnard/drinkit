---
name: backend-feature
description: Shapes a feature of DrinkIt's Kotlin backend as a vertical slice, from the API contract to the database, in the application's -domain, -infra and -backend modules, and proves its structure with the architecture tests. Use it before adding or changing backend production code, such as a use case, an entity or value type, a port, an adapter, a controller, an endpoint, a security rule, an HTTP client or an event of the User aggregate.
paths:
  - "**/*-domain/src/main/**/*.kt"
  - "**/*-infra/src/main/**/*.kt"
  - "**/*-backend/src/main/**/*.kt"
---

# A backend feature in DrinkIt

This skill says where each piece of a backend feature goes, in which order, and what to avoid. The model to copy
for each step is in [examples.md](examples.md). Tests come first: invoke the `test-driven-development` skill
before writing code. Its test order follows the steps below.

## Prove it with the architecture tests

Where each piece goes and what it may depend on are rules that tests check, not rules to memorize. Run them as soon
as the structure changes, a new class, package, port or module dependency, then again before you finish:

```
./gradlew :architecture-tests:test
./gradlew build
```

A broken rule fails the build with its id, its reason and its fix, and keeps the pull request red. Fix the code, as
`AGENTS.md` says, never the rule.

The tests do not check everything: a business value as a type rather than a primitive, a name in the words of the
domain, or a decision returned as a `Result` case. These follow
`docs/src/engineering/writing-code/best-practices/coding-rules.md` and
`docs/src/engineering/architecture/clean-architecture.md`, which the `issue-judge` agent and the reviewer apply. Other
pages of `docs/src/engineering/` explain a rule when it fails: `hexagonal-architecture.md`, `event-sourcing.md` before
touching the `User` aggregate, and `living-documentation.md` for the annotations each new kind of class carries.

## Order of work

`<root>` below is the package of the `@SpringBootApplication` of the application's `-backend` module, such as
`com.drinkit` for `drinkit-backend`.

1. **Contract.** A new or changed HTTP operation starts in the OpenAPI contract: the `api-contract-change`
   skill. The generated `<Tag>Api` interface gives the controller its signatures and mappings.
2. **Domain types**, in the application's `-domain` module, package `<root>.<context>.core`. A value several
   contexts use goes in `<root>.common`.
3. **Use case**, and a decider when its decisions deserve one, in `<root>.<context>`.
4. **Ports**, in `<root>.<context>.spi`, with their test doubles in the test fixtures of the `-domain` module.
5. **Adapters**, in the `-infra` module, in the package of the context. A new table or column first goes through
   the `database-change` skill.
6. **Controller**, in the `-backend` module, then its **security rule**.

A feature with no new HTTP operation, such as a platform event handler, skips steps 1 and 6.

## Use case

`clean-architecture.md` says what a use case holds, when a read needs one and when a decider pays. A use case on
the `User` aggregate appends events rather than saving a state: read [user-events.md](user-events.md).

## Controller and security rule

The controller builds the command from the request, calls the use case, and maps every `Result` case to a
status with an exhaustive `when`. A plain read with no rule to apply calls the port directly, from a method
annotated `@Transactional(readOnly = true)`, since a jOOQ adapter demands a running transaction: `UserController`.

Then open the `SecurityConfiguration` of the `-backend` module. The first matching rule wins, and paths are written
without the `server.servlet.context-path` of the backend's `application.yml`. In `drinkit-backend`, `/api/**`
requires `ROLE_USER` and a path no rule matches is denied: a new path under `/api/` that a registered user calls
needs no rule. A path that is public, reachable during registration, or for admins only gets its own line above
`/api/**`, which is a security rule: ask first, as `AGENTS.md` says.

## Before you finish

- [ ] `./gradlew :architecture-tests:test` and `./gradlew build` pass.
- [ ] A use case that changes something does the I/O, its decisions live in it or in a `@FunctionalCore` object,
      every decision maps to a `Result` case and every `Result` case to a status.
- [ ] Every operation of the contract is implemented by its controller, and `SecurityConfiguration` gives each
      new path the access it needs.
