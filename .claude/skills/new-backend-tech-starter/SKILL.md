---
name: new-backend-tech-starter
description: Creates a backend tech starter under tech-starters/backend, or brings an existing one up to convention, wired by its auto-configurations with no Spring profile, with its configuration properties, test fixtures and documentation. Use it when asked for a new tech starter or a reusable technical library for the backend.
paths:
  - "tech-starters/backend/**"
---

# New backend tech starter

A tech starter solves one technical concern under `tech-starters/backend/<name>-starter/` and knows no business word
(`docs/content/en/1.engineering/2.architecture/6.tech-starters.md`). It serves one application or several from its
first version, even while one uses it: nothing that belongs to one application is a constant of the starter (§3). The
module layout, the build file and the code shapes are in [examples.md](examples.md).

## Done means

- [ ] No dependency on an application module (`-domain`, `-infra`, `-backend`), and everything but the public
      API is `internal`.
- [ ] One `@AutoConfiguration` class, plus one per optional integration, all listed in `AutoConfiguration.imports`,
      does all the wiring. `src/main` holds no `@Service`, `@Component`, `@Repository` or `@Configuration`
      stereotype, and no Spring profile.
- [ ] Every bean is `@ConditionalOnMissingBean`, by name when its type is generic or shared, and
      `starters.<name>.enabled=false`, read by `@ConditionalOnBooleanProperty`, switches the starter off.
- [ ] Properties live under `starters.<name>.*`, constructor-bound in Kotlin, with a default only where it suits
      any application, and the README gives each one's environment variable.
- [ ] `testFixtures` ship a hand-written double of each public interface that consumers' tests use.
- [ ] `ApplicationContextRunner` tests cover the default bean, the back-off, the disabled starter and each
      implementation branch, and one `*TestContract` runs against both the double and the real implementation.
- [ ] `@TechStarterTool` marks the public API, and the README is written for the developer who consumes it.
- [ ] `./gradlew :<name>-starter:build` passes, and the application gets the beans from
      `implementation(project(":<name>-starter"))` alone, once the lock and the verification metadata are
      regenerated through the `dependency-change` skill.

## 1. Before writing anything

Read the convention plugins the build file needs, and one existing starter close to the need, from
`ls tech-starters/backend`. A starter with no bean, such as `kotlin-starter`, is a plain library with no
auto-configuration and no switch: that is not a gap.

Settle these from the issue, and ask the maintainer what it leaves open, a new module being a structuring choice:

1. The name, in kebab case and suffixed `-starter`, and the package `com.drinkit.<domain>`.
2. The problem, in one sentence. If it needs two, it is two starters.
3. The public API: what a business module injects.
4. One implementation, or several that the application chooses between.
5. Whether it owns PostgreSQL tables.
6. Its properties: every value that changes from one application to another, and which ones production must set.
7. Its test support: a double only, or also a Testcontainers JUnit extension.

Shape the public API for the code that will consume it:

- Writes and lifecycle go through the starter's own small interface, so that behavior can be plugged on them. A
  vendor SDK stays exposed for queries only.
- A method that needs the caller, a user, a visitor or a flag subject, reads it from the request. An overload that
  takes it explicitly exists only for code outside a request.
- One extension point per concern, merged into one interface rather than split across bean types.
- The design holds with several instances of the application running.
- A cross-cutting concern takes one generic mechanism, a hook of the framework first, the simplest that works.

## 2. Auto-configurations, no stereotype

The component scan of an application starts from the package of its `@SpringBootApplication`, `com.drinkit` for
`drinkit-backend`, which holds every starter package. A scanned bean ignores `@ConditionalOnMissingBean`, is
invisible to `ApplicationContextRunner`, is registered even when the starter is switched off, and disappears the
day an application with another base package imports the starter: `starters.no-stereotype` refuses it.

Traps:

- An `internal class` is fine, but its `@Bean` methods carry no `internal` modifier: Kotlin mangles the name of
  an internal function with a module suffix, and Spring no longer finds it. A bean of an `internal` type still
  backs off: Kotlin compiles `internal` to `public`.
- `@ConditionalOnMissingBean` without attributes matches the raw return type. A bean of a generic or shared type,
  such as `ApplicationListener<…>`, `RouterFunction<…>` or `HealthIndicator`, backs off by name, or any bean of the
  application of that type switches it off.
- Spring Boot 4 split its auto-configurations into modules and moved them:
  `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`, not
  `org.springframework.boot.autoconfigure.jdbc`. Check each import against the jar before writing it.
- Never add the starter to `spring.profiles.include`, and never use `@Profile`, even to choose an
  implementation: a starter that behaves differently in `dev` needs a property.

An optional integration, such as a health indicator, a servlet filter or the access rule of an endpoint, goes in an
`@AutoConfiguration` of its own, listed in the same `.imports` file, guarded by `@ConditionalOnClass` naming the
type by class, with its dependency declared `compileOnly`. Never in a nested `@Configuration`, which the
application's scan registers on its own, and which `starters.no-stereotype` refuses. A starter that serves an
endpoint brings its `EndpointRules` that way, outside `/api/**`, as `SseSecurityAutoConfiguration` does, and
`EndpointRulesCoverage` applies it in the application's coverage test.

Several implementations: one property selects, its default is the safest one, and one branch carries
`matchIfMissing = true` so the context always starts.

A starter made of features that depend on each other, as `security-starter` is, may keep one auto-configuration per
feature in a package per feature, each ordered by `after` on those it needs. Under `infra/`, the classes stay flat
while the starter uses one technology, and a subpackage named after each technology appears with the second one.
A concept that any technology of the starter would use, such as a retry, goes in `infra/<concept>`, never in the
package of one technology. One class per concept, and no internal interface that repeats the public API.
The auto-configuration and the properties are named `<Name>AutoConfiguration` and `<Name>Properties`, with
`Starter` before the suffix only when Spring Boot already has the name, as `CacheStarterProperties`.

## 3. Configuration

Properties go under `starters.<name>.*`, never under `spring.*`, `server.*` or `management.*`. The environment
variable is the property uppercased, with `.` turned into `_` and hyphens removed:
`starters.feature-management.flipt.url` is `STARTERS_FEATUREMANAGEMENT_FLIPT_URL`. A starter whose only property is
`enabled` reads it in `@ConditionalOnBooleanProperty` with no properties class, and its README documents it. A
property that chooses between values, such as a provider, stays in `@ConditionalOnProperty` with `havingValue`.

A value that belongs to one application, such as the URL of a service, a path, a name, a prefix or a table, is a
property, never a constant, even while one application uses the starter. Its default suits any application, or it
has none and the application fails at startup without it. A default that names DrinkIt is a sign it belongs to the
application.

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
instance. Keep that one for the business modules. A repository that a servlet filter calls, outside any use case,
as the sign-in codes of `security-starter`, opens its own transaction with `REQUIRED`.

## 5. Tests and fixtures

Tests come first, through the `test-driven-development` skill. The starter's tests have three layers: the wiring,
with `ApplicationContextRunner`; the behaviour, in a `*TestContract` run by each implementation and by the double,
like `ConfigurationsTestContract`; and the integration, with `@JooqIntegrationTest` or the starter's own extension.
A double of a public interface always runs the contract, however simple it is: when the real behaviour changes, the
double breaks with it. A test is named after the rule it proves, never after its example, and marks its
`// Given`, `// When` and `// Then` blocks.

A change to a starter is proved by the starter's own tests, never by a test through a business module in the same
pull request. A store that the tests need without the real technology lives in the test fixtures, never in
`src/main`. A public helper, constant or `api` dependency that nothing uses yet stays: the starter is a toolkit for
its consumers.

The fixtures are plain public classes that implement the public interface, so the compiler breaks them when the
API changes. The kinds and their models are in [examples.md](examples.md#test-fixtures).

## 6. Documentation

`@TechStarterTool` goes on every public interface, annotation and entry point of the API. The `README.md` at
the module root is the source of the generated page: follow the outline in
[examples.md](examples.md#readme). A concept that needs code to explain goes in an example under
`src/test/kotlin`, linked from the README, like `CarExample.kt` of `event-sourcing-starter`: a file with no class
of that name, which `git ls-files '*/CarExample.kt'` finds.

A KDoc says only what the name and the types cannot: a trap, an outside constraint, a reason. A class built to meet
an OWASP rule ends its KDoc with that rule, `OWASP: ASVS 5.0 V7.2.4, Session Management Cheat Sheet (id renewal)`,
so a reader sees why it exists. Wiring, or a class that only applies a rule another class holds, names none.

## 7. Aligning an existing starter

The starter keeps what teaches its use: its example classes, the bodies of its tests, its type parameter names and
its file layout. Test titles are renamed after the rule, and missing tests are added in the same style.
