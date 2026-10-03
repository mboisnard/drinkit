---
name: new-backend-tech-starter
description: Creates a backend tech starter under tech-starters/backend, or brings an existing one up to convention, wired by one auto-configuration with no Spring profile, with its configuration properties, test fixtures and documentation. Use it when asked for a new tech starter, a reusable technical library for the backend, or a migration of an existing starter away from profile-based activation.
paths:
  - "tech-starters/backend/**"
---

# New backend tech starter

A tech starter solves one technical concern under `tech-starters/backend/<name>-starter/` and knows no business
word (`tech-starters.md` under `docs/src/engineering/guidelines/`). The module layout, the build file and the
code shapes are in [examples.md](examples.md).

## Done means

- [ ] No dependency on a module of `drinkit/`, and everything but the public API is `internal`.
- [ ] One `@AutoConfiguration` class, listed in `AutoConfiguration.imports`, does all the wiring. `src/main` holds
      no `@Service`, `@Component`, `@Repository` or `@Configuration` stereotype, and no Spring profile.
- [ ] Every bean is `@ConditionalOnMissingBean`, and `starters.<name>.enabled=false` switches the starter off.
- [ ] Properties live under `starters.<name>.*`, constructor-bound with defaults in Kotlin, and the README gives
      each one's environment variable.
- [ ] `testFixtures` ship a hand-written double that consumers' tests use.
- [ ] `ApplicationContextRunner` tests cover the default bean, the back-off, the disabled starter and each
      implementation branch, and one `*TestContract` runs against both the double and the real implementation.
- [ ] `@TechStarterTool` marks the public API, and the README is written for the developer who consumes it.
- [ ] `./gradlew :<name>-starter:build` passes, and the application gets the beans from
      `implementation(project(":<name>-starter"))` alone.

## 1. Before writing anything

Read the convention plugins the build file needs, and one existing starter close to the need, from
`ls tech-starters/backend`. The existing starters predate this skill (component scan and a profile): copy their
tests and fixtures, never their wiring.

Settle these from the issue, and ask the maintainer what it leaves open, a new module being a structuring choice:

1. The name, in kebab case and suffixed `-starter`, and the package `com.drinkit.<domain>`.
2. The problem, in one sentence. If it needs two, it is two starters.
3. The public API: what a business module injects.
4. One implementation, or several that the application chooses between.
5. Whether it owns PostgreSQL tables.
6. Its properties, their defaults, and which ones production must set.
7. Its test support: a double only, or also a Testcontainers JUnit extension.

## 2. One auto-configuration, no stereotype

`DrinkitApplication` sits in `com.drinkit`, so its component scan covers every starter package. That is why the
older starters work with bare `@Service` annotations, and what must stop: a scanned bean ignores
`@ConditionalOnMissingBean`, is invisible to `ApplicationContextRunner`, is registered even when the starter is
switched off, and disappears the day an application with another base package imports the starter.

Traps:

- An `internal class` is fine, but its `@Bean` methods carry no `internal` modifier: Kotlin mangles the name of
  an internal function with a module suffix, and Spring no longer finds it.
- Spring Boot 4 split its auto-configurations into modules and moved them:
  `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`, not
  `org.springframework.boot.autoconfigure.jdbc`. Check each import against the jar before writing it.
- Never add the starter to `spring.profiles.include`, and never use `@Profile`, even to choose an
  implementation: a starter that behaves differently in `dev` needs a property.

Several implementations: one property selects, its default is the safest one, and one branch carries
`matchIfMissing = true` so the context always starts.

## 3. Configuration

Properties go under `starters.<name>.*`, never under `spring.*`, `server.*` or `management.*`. The environment
variable is the property uppercased, with `.` turned into `_` and hyphens removed:
`starters.feature-flags.flipt.url` is `STARTERS_FEATUREFLAGS_FLIPT_URL`. Do not reproduce the old
`${DRINKIT_XXX:}` placeholders, which hide a property from Spring's metadata.

A default for a key Spring owns, such as `spring.datasource.*`, goes in a defaults file loaded at the lowest
precedence by an `EnvironmentPostProcessor`, so that the application and the environment always win. In Spring
Boot 4 it is still registered in `META-INF/spring.factories`, unlike auto-configurations, under the
`org.springframework.boot` package and not the deprecated `org.springframework.boot.env` one.

## 4. Tables

Apply `com.drinkit.jooq-codegen-convention`, which brings `postgresql-starter`, and scope the generation to the
starter's tables like the `jooq` block of `configuration-starter`. The changelog and the codegen follow the
`database-change` skill.

The repository is registered by a `@Bean` method of the auto-configuration and carries
`@Transactional(propagation = Propagation.MANDATORY)`: the consumer's use case owns the transaction. Not
`@JooqRepository`, which is meta-annotated `@Repository`: under a component scan it would register a second
instance. Keep that one for the business modules.

## 5. Tests and fixtures

Tests come first, through the `test-driven-development` skill. The starter's tests have three layers: the wiring,
with `ApplicationContextRunner`; the behaviour, in a `*TestContract` run by each implementation, like
`ConfigurationsTestContract`; and the integration, with `@JooqIntegrationTest` or the starter's own extension.

The fixtures are plain public classes that implement the public interface, so the compiler breaks them when the
API changes. The kinds and their models are in [examples.md](examples.md#test-fixtures).

## 6. Documentation

`@TechStarterTool` goes on every public interface, annotation and entry point of the API. The `README.md` at
the module root is the source of the generated page: follow the outline in
[examples.md](examples.md#readme). A concept that needs code to explain goes in an example under
`src/test/kotlin`, like `CarExample.kt` in `event-sourcing-starter`, linked from the README.

## 7. Bringing an existing starter up to convention

One starter at a time, never mixing the two mechanisms in one module:

1. Add the `@AutoConfiguration` class and its `.imports` file, and move every `@Bean` into it.
2. Remove the stereotypes from the implementation classes.
3. Move `application-<name>.yml` to a defaults file behind an `EnvironmentPostProcessor` (§3), keeping the keys
   the application relies on.
4. Remove the profile from `spring.profiles.include`: last, once the `ApplicationContextRunner` tests and a
   `bootRun` both pass.
5. Rename the properties into `starters.<name>.*`, keeping the old keys bound and marked deprecated in the
   README until the application no longer sets them.
