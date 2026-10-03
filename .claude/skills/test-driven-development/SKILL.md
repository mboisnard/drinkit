---
name: test-driven-development
description: Drives a change to DrinkIt's code test first, in the Kotlin backend as in the Nuxt frontend. A test list in business language, one red-green cycle per behavior, then a required refactor phase, technical and functional, so the new behavior fits the whole feature. Use it before adding or changing behavior, for a feature as for a bug fix.
paths:
  - "drinkit/*/src/**/*.kt"
  - "tech-starters/backend/*/src/**/*.kt"
  - "drinkit/drinkit-frontend/src/**"
---

# Test-driven development in DrinkIt

Tests describe what the feature does, in the words of the business. The code is the simplest thing that makes
them pass, then it is reshaped until it reads like the domain. A behavior is never written before the test that
asks for it, and a refactor never changes what the tests assert.

The backend test conventions are in the Tests section of `AGENTS.md`, the code patterns in its Code patterns
section. They are not repeated here: read them before the first test.

## 1. Write the test list

Before any code, list the behaviors in `$(git rev-parse --path-format=absolute --git-path claude/test-list.md)`,
one line each, in the words of the issue and of the existing domain: what a user or a caller can observe, not
how the code does it. Each acceptance criterion maps to at least one line. Order them from the simplest to the
most involved. A test idea that comes up while coding goes in the list, never in a `TODO` comment.

### Backend

Work outside in, the way the hexagonal architecture is layered:

1. The use case, through its `invoke`, with the in-memory doubles that a `*Fixtures` class wires. Model:
   `CreateNewUserTest` and `UserFixtures` in `drinkit-domain`. Its decisions are tested in a `@Nested` class.
2. A new port: its behavior written once in an abstract `*TestContract` of the domain test fixtures, run by a
   test of the in-memory double. Model: `ExchangeRatesTestContract` and `InMemoryExchangeRatesTest`.
3. The adapter: a test in `drinkit-infra` that runs the same contract against the real technology. Model:
   `JooqExchangeRatesRepositoryTest`, with `@JooqIntegrationTest`.
4. The controller last, once the use case does what it should.

A single test: `./gradlew :drinkit-domain:test --tests '*CreateNewUserTest'`.

### Frontend

Work from what the user sees and does:

1. A page or component test renders it, acts as a user would, clicks and types, and checks what appears. It
   finds elements by role, label or text, never by CSS class or component internals.
2. Logic that is not rendering, a validation, a formatting, the state of a composable, is tested as plain
   functions.
3. The backend is reached through the client generated from the OpenAPI contract. A test gives the page a
   hand-written fake of that client, never a mock of a module.

The frontend has no test runner yet (#400). Until it has one, a frontend criterion is proven by a check
command, such as `npm run generate:client-api && npm run build`, and what only a person can see becomes a
step for the maintainer in the pull request.

## 2. One cycle per behavior

If the next behavior does not fit the code as it is, commit what is already green, then reshape that code,
tests green, and commit the reshape on its own: make the change easy, then make the easy change. Then take the next line of the list, and
only that one.

1. **Red.** Write one test for it. Run it alone and read the failure. It must fail because the behavior is
   missing, not because of a typo, a missing import or a broken fixture. Note the failing line next to the
   behavior in the test list.
2. **Green.** Write the simplest code that makes it pass, then run the tests of the module.
3. Tick the line and move to the next.

Rules that hold at every step:

- Production code written before its test is deleted, then written again from the test.
- A test that passes at once is a warning: either the behavior already exists or the test checks nothing.
  Find out which before going on.
- Never weaken, skip or delete an assertion to get green. If a test looks wrong, say so to the maintainer
  rather than working around it.
- Write a solution that works for every valid input, not one shaped after the test values.
- No mock: test doubles are written by hand, in `src/testFixtures` for the backend. An assertion never
  recomputes its expected value with the code under test.
- One behavior per test. A test name with "and" in it is two tests.

## 3. Refactor

This phase is required, even when the code looks good enough. It reshapes the code the branch added. Every
step keeps the tests green, and no assertion changes. A test name may follow a rename of the domain, since the
names are the specification. If the phase shows that code older than the branch should change too, commit the
behavior first, then reshape that older code in a commit of its own.

### Technical

- Does each unit, a class, a component or a composable, have a single reason to change?
- Backend: does the use case only do I/O, with every decision in its functional core, pure and returning a
  sealed decision?
- Frontend: does the page only compose, the components only display, and the logic live in composables or
  plain functions rather than in templates?
- Is there an abstraction used once, a parameter nobody passes, or a helper for one call? Inline it.
- Is a rule, a mapping or a test setup duplicated? Keep one.
- Is a business value carried by a primitive or a loose string where a type would say what it is?
- Does each name say what the thing is in the domain, never its pattern?

### Functional

- Do classes, components, functions and tests use the words of the issue and of the existing domain, the same
  word for the same concept everywhere, on screen as in the API contract?
- Is a business concept hidden in a condition, a flag or a string? Name it: a type, a case of a sealed
  decision, a named predicate.
- Does each business rule live in one place, the functional core or the type that owns it? The frontend shows
  the outcome of a rule the backend decides, it does not decide it again.
- Does the new behavior fit the feature as a whole? If an existing use case, aggregate, port, page or component
  has to bend to take it, reshape that one rather than adding a parallel path.
- Read the test names of the feature in a row: do they read like its specification, with no gap and no
  duplicate?
- Did the new model make something obsolete, a type, a branch, a component, a test? Delete it.

## 4. Commit

Commits follow `AGENTS.md`, grouped by topic: a behavior and the refactor of its own code go in one commit, once
section 3 is done. Every commit compiles and passes its tests. A reshape of code older than the branch never
shares a commit with new behavior: commit what is green first, then the reshape on its own, before the next
behavior that needs it or after the refactor phase that revealed it.

## What the judge checks

The `issue-judge` agent reads the test list, the tests and the diff. It checks that every acceptance criterion
has its test or its check command, that the tests state behaviors, that the refactor phase above was done, and
that no commit mixes a reshape of older code with new behavior.
