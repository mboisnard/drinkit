---
name: test-driven-development
description: Drives a change to DrinkIt's code test first, in the Kotlin backend as in the Nuxt frontend. A test list in business language, one red-green-tidy cycle per behavior, then a required refactor of the whole feature, technical and functional. Use it before adding or changing behavior, for a feature as for a bug fix.
paths:
  - "drinkit/*/src/*/kotlin/**/*.kt"
  - "tech-starters/*/*/src/*/kotlin/**/*.kt"
  - "drinkit/*-frontend/src/**"
---

# Test-driven development in DrinkIt

## Why this loop

- **Tests are the specification.** Their names, read in a row, say what the feature does in the business's words.
- **The design emerges from small steps.** Each test forces only the code it needs, nothing for a future test.
- **Refactoring is continuous.** After every green, and once more for the whole feature, without changing what
  the tests assert.

When a situation is not covered below, decide by these three ideas.

## 1. Write the test list

Before any code, list the behaviors in `$(git rev-parse --path-format=absolute --git-path claude/test-list.md)`,
or extend the list `implement-issue` seeded from the issue's "Verification" field. One line each, in the words of
the issue and of the existing domain: what a user or a caller observes, not how the code does it. Each acceptance
criterion has at least one line, with its test or its check command, and so does each edge case you can think of.
A test idea that comes up while coding goes in the list, never in a `TODO` comment.

Then read how tests start on the side you touch: [backend.md](backend.md) or [frontend.md](frontend.md). On the
backend, the shapes of a test and of its Kotest assertions are in
[the examples of `test-existing-code`](../test-existing-code/examples.md).

## 2. One cycle per behavior

Pick the next line: the simplest one that fails today, then one that forces the code to generalize, and the
unhappy paths early: not found, refused, invalid. When the line does not fit the code as it is, reshape that code
first, tests green: make the change easy, then make the easy change.

1. **Red.** Write one test for that line. Run it alone and read the failure: it fails because the behavior is
   missing, not because of a typo, an import or a broken fixture.
2. **Green.** Write the simplest code that passes, even a constant: the next test, chosen to generalize, removes
   it. Then run the tests of the module.
3. **Tidy.** Clean what this cycle touched, tests green: a clearer name, a duplication, a test setup that grew.
   No new behavior here.
4. Tick the line and pick the next.

The loop ends when every line is ticked and you cannot think of a failing test worth writing.

Rules at every step:

- Production code written before its test is deleted, then written again from the test.
- A test that passes at once is a warning: either the behavior exists or the test checks nothing. Find out which.
- Never weaken, skip or delete an assertion to get green. A test that looks wrong is reported to the maintainer.
- An assertion never recomputes its expected value with the code under test.
- One behavior per test: a name with "and" in it is two tests.
- Tests are fast, independent and repeatable: no sleep, no shared mutable state, time and identifiers from test
  doubles.
- Once the list is done, no value shaped after a test remains: the code works for every valid input.

## 3. Refactor the feature

This phase is required, even when the code looks good enough: agents tend to skip it. It reshapes the code the
branch added, as a whole, tests green at every step and no assertion changed. A test name may follow a rename of
the domain, since the names are the specification. The Code patterns of `AGENTS.md` hold for the whole diff.

### Technical

- Does each unit, a class, a component or a composable, have a single reason to change?
- Frontend: does the page only compose, the components only display, and the logic live in composables or plain
  functions rather than in templates?
- Is there an abstraction used once, a parameter nobody passes, or a helper for one call? Inline it.
- Is a rule, a mapping or a test setup duplicated? Keep one.

### Functional

- Do classes, components, functions and tests use the words of the issue and of the existing domain, the same word
  for the same concept everywhere, on screen as in the API contract?
- Is a business concept hidden in a condition, a flag or a string? Name it: a type, a case of a sealed decision,
  a named predicate.
- Does each business rule live in one place, the functional core or the type that owns it? The frontend shows the
  outcome of a rule the backend decides, it does not decide it again.
- Does the new behavior fit the feature as a whole? When an existing use case, aggregate, port, page or component
  has to bend to take it, reshape that one rather than adding a parallel path.
- Read the test names of the feature in a row: do they read like its specification, with no gap and no duplicate?
- Did the new model make something obsolete, a type, a branch, a component, a test? Delete it.

## 4. Commit

A behavior and the refactor of its own code go in one commit, once section 3 is done. Every commit compiles and
passes its tests. A reshape of code older than the branch never shares a commit with new behavior: commit what is
green first, then the reshape on its own, before the behavior that needs it or after the refactor that revealed
it.
