# 0001. Build with Gradle and shared conventions

- Status: accepted
- Date: 2026-10-24

The backend builds with Gradle, its scripts written in Kotlin, and what its modules share lives in convention plugins
that each module applies. A new tech starter or a new application then builds like the others from its first commit,
and its build file only says what makes it different.

## Context

The backend grows one module at a time: the application's domain, its adapters and its Spring Boot backend, a tech
starter per technical concern, the database updater, the architecture tests. Each one needs the same Kotlin and Java
setup, the same code analysis, the same tests and the same dependency versions. Copied from one build file to the
next, that setup drifts, and a fix has to be repeated in each copy.

The build also runs in CI on every pull request, so its time counts, and the tools of the stack, Spring Boot, GraalVM,
KSP, jOOQ, OpenAPI Generator, detekt and Konture, all have to plug into it.

## Decision

DrinkIt builds its backend with Gradle, as the [build tool page](../build-and-data/build-tool.md) describes:

- **Build logic in Kotlin.** Build scripts and convention plugins are written in the language of the code, and detekt
  checks them like the rest of it.
- **Reuse through conventions.** A convention plugin under `build-logic/` holds one concern. A module applies one
  archetype for its kind, such as a library or a Spring Boot application, then the add-ons it needs, and never configures
  Kotlin, Java or its repositories itself.
- **One place for every version.** The libraries and plugins take their versions from `gradle/platform/`, and Gradle
  locks them and verifies the checksum of what it downloads.
- **Skipped work.** The build cache and the configuration cache skip what an earlier build already did.

The Nuxt frontend and the documentation site build with npm, outside Gradle.

## Consequences

- Two modules of the same kind cannot drift apart, and a change to a convention reaches every module that applies it.
- A new module joins the build by its folder, with a build file of a few lines.
- A small change in a large build rebuilds little.
- A convention has to keep the configuration cache working, and a mistake there slows every build down.
- Gradle's API is larger than Maven's, and a convention plugin is code that someone has to read and maintain.
- Dependency locking and verification make each dependency change carry its new checksums and lock.

## Alternatives

- **Maven.** Rejected because a module inherits from one parent POM only, so shared settings cannot be combined per
  module as convention plugins are, and Maven rebuilds what has not changed. It offered a well-known, declarative
  build.
- **Gradle without conventions, each module configured in its own build file.** Rejected because the copies drift
  apart, and a fix has to be repeated in every module. It offered no plugin to maintain.

## Sources

- [Sharing build logic between subprojects](https://docs.gradle.org/current/userguide/sharing_build_logic_between_subprojects.html), Gradle
- [Best practices](https://docs.gradle.org/current/userguide/best_practices.html), Gradle
- [Introduction to the POM](https://maven.apache.org/guides/introduction/introduction-to-the-pom.html), Apache Maven,
  the inheritance model of the alternative
