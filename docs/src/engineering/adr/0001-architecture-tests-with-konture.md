# 0001. Architecture tests with Konture

- Status: accepted
- Date: 2026-10-11
- Issue: [#398](https://github.com/mboisnard/drinkit/issues/398)

## Context

DrinkIt's architecture rules lived as prose in `AGENTS.md`, the guidelines and the skills, and only the Gradle module graph, Kotlin `internal` and detekt were checked. #398 listed the rules first, then compared the tools against them.

A third of the rules need what only the Kotlin source holds: `internal`, `data` and `object`, and the documentation annotations, retained in `SOURCE` only. Several need the Gradle module graph. A package spans modules, `com.drinkit.user` being in the domain, infra and backend, so a rule targets a module, not a package alone.

## Decision

The rules are Konture tests in a dedicated `architecture-tests` module, one file per theme, one test per rule written in Konture's DSL against the real modules. Each new rule is shown failing once on a violation written for the occasion, as Konture's guide advises, rather than kept with a permanent fixture. Konture is applied to the root project and to `architecture-tests` only, not from the settings, so the tests of the other modules never depend on the module graph it builds.

A rule selects modules by convention, never by name, since the monorepo may hold several applications sharing the tech starters: an application's modules by their suffix, `-domain`, `-infra` or `-backend`, and the starters by theirs, `-starter`, never by a folder, so that a renamed or moved module stays checked. Konture's class, file and function rules select modules by exact path, so the tests compute these lists from its module graph. A new application is checked from its first build, and one rule keeps applications from depending on each other.

Violations older than a rule are tolerated in the rule itself, by name and with a reason, through Konture's programmatic suppressions. Its JSON baseline records the line of each violation, which failed the build on any edit around a tolerated one.

The rules a single file shows, forbidden imports and annotations, stay in Konture too, although detekt could check them: every rule then has one place, one id and one description.

## Consequences

- The rules see `internal`, the annotations and the module graph, without compiling anything or starting Spring.
- Konture resolves types from imports, without type inference, so a rule reads declared types and calls.
- Konture 1.0.0 was published on 2026-09-23, from a repository started on 2026-07-10, by one main author who tests on JDK 21; it runs here on JDK 25. It parses with the Kotlin compiler it embeds, so a Kotlin upgrade waits for a Konture release that parses the new syntax.
- Two build changes came with it: GraalVM's native test support is off, since Konture reads every project's dependencies from the root project and that classpath only resolves from its own project, and the OpenAPI sources are declared by folder rather than by task.
- Rules on the build files, `build-logic` and the resources stay outside, since Konture reads none of them.
- Konture 1.0.2 lists every module's sources at configuration time to find rules on external libraries, so a Kotlin edit discarded Gradle's configuration cache: the convention empties that task's input, since no rule here reads an external library. Its analysis cache cannot be switched on yet, the plugin passing its settings to the tests as unreadable values.
- A suppression tolerates every violation of its rule by that class or file, not only the one it was written for, and a slice suppression may hide another cycle through the same packages, so a slice that other rules guard is left out of the cycle rules instead. Nothing fails when that violation is fixed: its line is deleted by hand.

## Alternatives

- ArchUnit 1.5: mature, but reads bytecode, where `internal` is public and the annotations are gone, and it misses the dependencies of the `when` expressions Kotlin 2.4.20 compiles to a type switch (TNG/ArchUnit#1699).
- Taikai: ready-made ArchUnit rules, whose Spring conventions require the `Service` and `Repository` suffixes DrinkIt forbids.
- Spring Modulith: checks module cycles and boundaries only, by package under the application package, which here would make every starter a module.
- Konsist: no release since 0.17.3, 2024-12-08.
