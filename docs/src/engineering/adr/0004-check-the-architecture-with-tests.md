# 0004. Check the architecture with tests

- Status: accepted
- Date: 2026-10-25

The structure DrinkIt's code follows, its patterns and the rules that keep them, is checked by architecture tests
written with Konture, which fail the build when a change breaks a rule. A rule that only a page states is soon broken
by a person in a hurry or by a coding agent that never read the page.

## Context

DrinkIt builds on a few patterns that each ask something of the code:

- the [hexagonal architecture](../architecture/hexagonal-architecture.md): the domain depends on nothing, and its
  adapters plug into its ports
- the [clean architecture](../architecture/clean-architecture.md): one use case per business action, with a
  functional core where it pays
- the [modular monolith](../architecture/modular-monolith.md): bounded contexts that share no internals
- [event sourcing](../architecture/event-sourcing.md) and CQRS for the aggregates whose history matters, and
  [platform events](../architecture/platform-events.md) between contexts
- the [tech starters](../architecture/tech-starters.md), which hold technical code and no business code

Beneath them, the everyday practices of the [coding rules](../writing-code/best-practices/coding-rules.md),
[SOLID](../writing-code/best-practices/solid.md), DRY and KISS, and the
[scout rule](../writing-code/best-practices/scout-rule.md). These rules lived as prose in the pages and the agents'
skills, and only the Gradle module graph and Kotlin's `internal` enforced any of them.

Many rules need what only the Kotlin source holds, such as `internal`, `data` or the annotations that mark the
patterns, and many need the module graph, since a package spans the domain, infra and backend modules.

## Decision

Each structural rule is a test of the `architecture-tests` module, written with Konture against the real modules,
one file per theme and one test per rule, as the [architecture tests page](../writing-code/testing/architecture-tests.md)
lists them. A rule finds the modules by convention, an application's by their suffix such as `-domain` and the
starters by `-starter`, so a new application or starter is checked from its first build. A violation older than a rule
is tolerated in the rule itself, by name and with its reason, never by loosening the rule.

The practices that no tool can judge, such as KISS or a good name, stay on their pages and in review.

## Consequences

- The build fails on a broken rule with its id, its reason and how to fix it, for a person and an agent alike.
- The rules read the Kotlin sources and the module graph without compiling or starting Spring.
- Konture is young and has one main author. It parses with the Kotlin compiler it embeds, so a Kotlin upgrade waits
  for a Konture release that reads the new syntax.
- Konture resolves types from the imports, without type inference, so a rule sees declared types and calls only.
- The build files, `build-logic` and the resources stay out of the rules, left to review.

## Alternatives

- **ArchUnit.** Rejected because it reads the bytecode, where Kotlin's `internal` is public and source annotations are
  gone, so rules on visibility and annotations would lie. It offered the most mature tool.
- **Spring Modulith.** Rejected because it checks module boundaries by package only, which here would make every
  starter a module. It offered checks integrated with Spring.
- **Konsist.** Rejected because it has had no release since the end of 2024. It offered a source-based tool close to
  Konture.

## Sources

- [Konture](https://github.com/baole/konture), its repository and guide
- [ArchUnit](https://www.archunit.org/), the bytecode-based alternative
- [Architectural fitness function](https://www.thoughtworks.com/radar/techniques/architectural-fitness-function), Thoughtworks Technology Radar
