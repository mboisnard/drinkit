---
name: backend-feature
description: Shapes a feature of DrinkIt's Kotlin backend as a vertical slice, from the API contract to the database, in drinkit-domain, drinkit-infra and drinkit-backend, and points to the coding rules and architecture pages it follows. Use it before adding or changing backend production code, such as a use case, an entity or value type, a port, an adapter, a controller, a security rule, an HTTP client or an event of the User aggregate.
paths:
  - "**/*-domain/src/main/**/*.kt"
  - "**/*-infra/src/main/**/*.kt"
  - "**/*-backend/src/main/**/*.kt"
---

# A backend feature in DrinkIt

This skill says where each piece of a backend feature goes, in which order, and what to avoid. The model to copy
for each step is in [examples.md](examples.md). Tests come first: invoke the `test-driven-development` skill
before writing code. Its test order follows the steps below.

## The pages to apply

The rules of backend code live in these pages. Read each one before the code it covers and apply it as it stands:

- `docs/src/engineering/writing-code/best-practices/coding-rules.md`, before naming or typing anything: strong
  typing, idempotency, the language of the domain, testability.
- `docs/src/engineering/architecture/clean-architecture.md`, before writing a use case: what it holds, and the
  Functional Core, Imperative Shell split between its decisions and its I/O.
- `docs/src/engineering/architecture/hexagonal-architecture.md`, before adding a port or a module dependency.
- `docs/src/engineering/architecture/event-sourcing.md`, before touching the `User` aggregate.
- `docs/src/engineering/writing-code/best-practices/living-documentation.md`, which lists the annotations of
  `documentation-starter` that mark each pattern in the code.

The architecture tests enforce the dependency rules: `docs/src/engineering/writing-code/testing/architecture-tests.md`
gives each rule's reason and fix. What they cannot check, such as strong typing, is checked against these pages by
the `issue-judge` agent and by the reviewer of the pull request.

Two of these rules hold in every class:

- Annotate new classes with the `documentation-starter` annotations. `@CoreDomain`, `@Usecase` and
  `@TechStarterTool` generate the documentation pages, the others mark a pattern in the code.
- Business values are types, never primitives. An input value exposes `validate()`, whose errors the use case, or
  its functional core, turns into a decision. An identifier or an invariant checks itself in `init`.

## Order of work

1. **Contract.** A new or changed HTTP operation starts in the OpenAPI contract: the `api-contract-change`
   skill. The generated `<Tag>Api` interface gives the controller its signatures and mappings.
2. **Domain types**, in `drinkit-domain`, package `com.drinkit.<context>.core`. A value several contexts use
   goes in `com.drinkit.common`.
3. **Use case**, and its functional core when it takes decisions, in `com.drinkit.<context>`.
4. **Ports**, in `com.drinkit.<context>.spi`, with their test doubles in the test fixtures of `drinkit-domain`.
5. **Adapters**, in `drinkit-infra`, in the package of the context. A new table or column first goes through
   the `database-change` skill.
6. **Controller**, in `drinkit-backend`, then its **security rule**.

A feature with no new HTTP operation, such as a platform event handler, skips steps 1 and 6.

## Use case

The use case gathers the inputs (ports, the current date, new ids), hands them to its functional core if it has
one, does what the decision says and maps it to a `Result`. Every decision becomes a `Result` case the controller
can answer. A decision handled with `error()` answers 500: never copy such a branch, even from a model.

A read that decides nothing is a query: no command and no core, under `@Transactional(readOnly = true)`.

Keep the use case in the parent package of `core`: the documentation generator attaches a `@Usecase` to the
`@CoreDomain` of `<context>.core` through that package.

A use case on the `User` aggregate appends events rather than saving a state: read [user-events.md](user-events.md).

## Controller and security rule

The controller builds the command from the request, calls the use case, and maps every `Result` case to a
status with an exhaustive `when`. A plain read with no rule to apply may call the port directly, from a method
annotated `@Transactional(readOnly = true)`, since a jOOQ adapter demands a running transaction: `UserController`.

Then open `SecurityConfiguration` in `drinkit-backend`. The first matching rule wins, paths are written without
the `/drinkit` context path, and `/api/**` requires `ROLE_USER`. A new path under `/api/` that a registered user
calls needs no rule. A path that is public, reachable during registration, or for admins only gets its own line
above `/api/**`, which is a security rule: ask first, as `AGENTS.md` says. A path no rule matches is denied.

## Traps

- `@PreAuthorize` does nothing while method security is off, which `git grep -n EnableMethodSecurity -- '*.kt'`
  tells. Until it is on, `SecurityConfiguration` alone decides access.
- `CustomResponseEntityExceptionHandler` decides the status of an exception: an `IllegalArgumentException`,
  thrown by a `require` in an `init`, answers 400, and an exception it does not handle answers 500.
- A use case with no documentation annotation, or returning no sealed `Result`, predates these patterns: it is
  not a model, whatever its context.

## Before you finish

- [ ] Each new class sits in the module and package of its step, and each adapter is `internal`.
- [ ] The use case does the I/O, its decisions live in it or in a `@FunctionalCore` object, every decision maps
      to a `Result` case and every `Result` case to a status.
- [ ] Every operation of the contract is implemented by its controller, and `SecurityConfiguration` gives each new path
      the access it needs.
