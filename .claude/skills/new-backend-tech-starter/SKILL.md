---
name: new-backend-tech-starter
description: Create a new backend tech-starter under tech-starters/backend, or bring an existing one up to convention — auto-configuration, configuration properties, test fixtures, generated documentation. Use when asked for a new tech-starter, a reusable technical library for the backend, or a migration of an existing starter away from profile-based activation.
---

# New Backend Tech-Starter

Create a new backend tech-starter in this monorepo, or bring an existing one up to the current
conventions.

A **tech-starter** is an isolated technical library under `tech-starters/backend/<name>-starter/`.
It solves one infrastructure concern (security, SSE, monitoring, persistence, messaging…) so that
business modules never touch the underlying technology. It knows nothing about the business domain.

Stack as of this writing: Kotlin on a Java 25 toolchain, Spring Boot 4.1, jOOQ, Gradle with
convention plugins in the `build-logic` included build, detekt, KSP-generated documentation.

---

## 0. Before writing anything

Read these files first — they are the ground truth and they drift:

- `build-logic/src/main/kotlin/archetype/com.drinkit.library-convention.gradle.kts`
- `build-logic/src/main/kotlin/archetype/com.drinkit.common-convention.gradle.kts`
- `build-logic/src/main/kotlin/test/com.drinkit.test-fixtures-convention.gradle.kts`
- `build-logic/src/main/kotlin/test/com.drinkit.test-convention.gradle.kts`
- `build-logic/src/main/kotlin/quality/com.drinkit.documentation-convention.gradle.kts`
- `build-logic/src/main/kotlin/database/com.drinkit.jooq-codegen-convention.gradle.kts`
- `gradle/platform/libs.versions.toml` and `gradle/platform/build.gradle.kts` (the internal BOM)
- One existing starter close to the need: `feature-flags-starter` (properties, health, filter),
  `configuration-starter` (jOOQ, test contract, fixtures), `search-engine-starter` (Testcontainers
  extension), `mail-starter` (two implementations).

Then ask the user, using the question tool, and do not start before you have answers:

1. **Name** — kebab-case, always suffixed `-starter`. Kotlin package `com.drinkit.<domain>`.
2. **Problem solved** — one sentence. If it needs two, it is probably two starters.
3. **Public API** — what a business module injects: interface names, method signatures.
4. **Implementations** — one, or several selectable by the consuming application?
5. **Persistence** — does it own PostgreSQL tables?
6. **Configuration** — which properties, which defaults, which are mandatory in production?
7. **Test support** — in-memory fixture only, or also a Testcontainers JUnit extension?
8. **Dependencies on other starters** — `kotlin-starter` (utils, logging), `documentation-starter`
   (annotations, comes with the convention), `postgresql-starter`, `messaging-starter`, …

---

## 1. The contract — non-negotiable rules

| # | Rule | Why |
|---|---|---|
| 1 | No dependency on `drinkit-domain`, `drinkit-infra`, `drinkit-backend` | a starter is business-agnostic and reusable |
| 2 | Everything except the public API is `internal` | the consumer cannot couple to an implementation |
| 3 | **No `@Service` / `@Component` / `@Repository` / `@Configuration` in `src/main`** | see §4 — the app component-scans `com.drinkit.**` |
| 4 | Wiring goes through **one `@AutoConfiguration` class** listed in `AutoConfiguration.imports` | explicit, ordered, conditional, testable |
| 5 | **No Spring profile** to activate a starter, ever | profiles are an application concern, not a library one |
| 6 | Every bean is `@ConditionalOnMissingBean` | the application can always override |
| 7 | Every starter is switchable off by `starters.<name>.enabled` | an app must never be held hostage by the starter's release cycle |
| 8 | Properties under `starters.<name>.*` — never under `spring.*`, `server.*`, `management.*` | Spring owns those namespaces and will change them |
| 9 | Every property overridable by environment variable, documented in the README | ops choose ConfigMap or env injection |
| 10 | Internal logic is unit-tested inside the starter | the consumer trusts it blindly |
| 11 | `testFixtures` ship a real in-memory/spy implementation — never MockK | one consistent test double for the whole application, not per-test mocks |
| 12 | `README.md` at the module root + `@TechStarterTool` on the public API | feeds the generated VitePress documentation |

---

## 2. Module layout

```
tech-starters/backend/<name>-starter/
├── build.gradle.kts
├── README.md                                   ← documentation source (VitePress)
└── src/
    ├── main/
    │   ├── kotlin/com/drinkit/<domain>/
    │   │   ├── XxxService.kt                   ← public API, @TechStarterTool
    │   │   ├── XxxTypes.kt                     ← public value types used by the API
    │   │   └── infra/
    │   │       ├── XxxAutoConfiguration.kt     ← internal, the ONLY wiring entry point
    │   │       ├── XxxProperties.kt            ← internal @ConfigurationProperties
    │   │       └── DefaultXxxService.kt        ← internal implementation(s), NO stereotype
    │   └── resources/META-INF/spring/
    │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    ├── test/kotlin/com/drinkit/<domain>/
    │   ├── XxxAutoConfigurationTest.kt         ← ApplicationContextRunner, wiring contract
    │   ├── XxxTestContract.kt                  ← shared behaviour across implementations
    │   └── DefaultXxxTest.kt / JooqXxxTest.kt
    └── testFixtures/kotlin/com/drinkit/<domain>/
        ├── InMemoryXxx.kt                      ← stateful behaviour for consumers' tests
        ├── SpyXxx.kt                           ← side-effect capture
        └── XxxIntegrationTest.kt               ← @ExtendWith meta-annotation, if Testcontainers
```

Gradle auto-discovers the module (`settings.gradle.kts` scans `tech-starters/backend`). The project
path is flat: `:<name>-starter`, not `:tech-starters:backend:<name>-starter`.

---

## 3. build.gradle.kts

```kotlin
plugins {
    id("com.drinkit.library-convention")        // Spring beans → spring-context, spring-tx,
                                                // kotlin-starter, event-sourcing-starter, KSP docs
    id("com.drinkit.test-fixtures-convention")  // only if you ship testFixtures
    id("com.drinkit.jooq-codegen-convention")   // only if you own PostgreSQL tables
}

dependencies {
    // Needed to write @AutoConfiguration / @ConditionalOn*
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    // api            → the type appears in your PUBLIC API signatures
    // implementation → internal only
    // compileOnly    → optional integration, guarded by @ConditionalOnClass
    api(libs.some.client)
    implementation(project(":kotlin-starter"))
    compileOnly("org.springframework.boot:spring-boot-starter-actuator")

    // testFixturesApi for anything a consumer's test classpath needs transitively
    testFixturesApi(libs.testcontainers.something)
}
```

Convention picker:

| Need | Plugin |
|---|---|
| Spring beans (the normal case) | `com.drinkit.library-convention` |
| Pure utilities, annotations, no Spring | `com.drinkit.common-convention` (+ `com.drinkit.documentation-convention` to appear in the docs) |
| Ships in-memory / spy / JUnit extensions | `+ com.drinkit.test-fixtures-convention` |
| Owns PostgreSQL tables | `+ com.drinkit.jooq-codegen-convention` |

`test-convention` (applied by `common-convention`) already puts `test-starter` on the test classpath,
which brings `spring-boot-starter-test`, Kotest and JUnit 5 — `ApplicationContextRunner` needs no
extra dependency.

Versions: never write one. The internal BOM (`gradle/platform`) is applied to every source set by
`common-convention`. A new third-party library goes into `gradle/platform/libs.versions.toml` and, if
the Spring Boot BOM does not manage it, into the `constraints` block of
`gradle/platform/build.gradle.kts`.

---

## 4. Auto-configuration — the part that must change from the old starters

The application class `com.drinkit.DrinkitApplication` sits in package `com.drinkit`, so its
component scan covers **every starter package**. That is why the existing starters "work" with bare
`@Service` annotations — and it is exactly what must stop. A component-scanned bean ignores
`@ConditionalOnMissingBean`, is invisible to `ApplicationContextRunner`, is registered even when the
starter is switched off, and breaks silently the day an application with a different base package
imports the starter.

**A new starter exposes exactly one auto-configuration and no stereotype annotation.**

`src/main/kotlin/com/drinkit/<domain>/infra/XxxAutoConfiguration.kt`:

```kotlin
package com.drinkit.notification.infra

import com.drinkit.notification.NotificationSender
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnClass(SomeVendorClient::class)
@ConditionalOnProperty(prefix = "starters.notification", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(NotificationProperties::class)
internal class NotificationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun notificationSender(properties: NotificationProperties): NotificationSender =
        DefaultNotificationSender(properties.endpoint, properties.timeout)
}
```

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.drinkit.notification.infra.NotificationAutoConfiguration
```

Points that bite:

- **`internal class` is fine.** Kotlin compiles an `internal` class to a public JVM class with no name
  mangling, so Spring instantiates it. But `@Bean` methods inside it must **not** carry the `internal`
  modifier — internal *functions* are mangled with a module suffix and Spring will not find them.
  Declare them with no modifier.
- `@AutoConfiguration` is meta-annotated `@Configuration(proxyBeanMethods = false)`. Never call one
  `@Bean` method from another; take the dependency as a parameter.
- Ordering: `@AutoConfiguration(after = [DataSourceAutoConfiguration::class])`. Spring Boot 4 split
  `spring-boot-autoconfigure` into ~70 modules and moved the classes, so it is
  `org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration`, not
  `org.springframework.boot.autoconfigure.jdbc.…`. Check the import before writing it.
- Optional integrations (actuator health indicator, servlet filter) go in a nested
  `@Configuration(proxyBeanMethods = false)` class guarded by `@ConditionalOnClass` /
  `@ConditionalOnWebApplication`, with the dependency declared `compileOnly`.
- Keep the auto-configuration cheap: no network call, no file read, no eager singleton work. It runs
  on every startup of every application importing the starter.
- To fail fast on invalid configuration, validate in the properties class (`@Validated` +
  `jakarta.validation`) rather than at first use.

**Never** add the starter to `spring.profiles.include` in
`drinkit/drinkit-backend/src/main/resources/application.yml`. A correctly built starter activates by
being on the classpath, and by nothing else.

---

## 5. Configuration properties and environment variables

Namespace `starters.<name>.*`. One `internal data class`, constructor-bound, defaults in Kotlin:

```kotlin
package com.drinkit.notification.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "starters.notification")
internal data class NotificationProperties(
    val enabled: Boolean = true,
    val endpoint: String = "",
    val timeout: Duration = Duration.ofSeconds(5),
    val provider: Provider = Provider.DEFAULT,
) {
    enum class Provider { DEFAULT, VENDOR }
}
```

This gives ops both override routes with no extra work, thanks to Spring's relaxed binding:

| Route | Example |
|---|---|
| YAML (ConfigMap, `application.yml`) | `starters.notification.timeout: 10s` |
| Environment variable | `STARTERS_NOTIFICATION_TIMEOUT=10s` |

The env var name is the property with `.` replaced by `_`, hyphens **removed**, uppercased —
`starters.feature-flags.flipt.url` becomes `STARTERS_FEATUREFLAGS_FLIPT_URL`. Prefer single-word
segments in the namespace so the mapping stays obvious, and **document the exact env var name of
every property in the README**: that is the contract with ops.

Do not reproduce the old `${DRINKIT_XXX:}` placeholder style. It exists only to rename the env var and
it hides the property from Spring's metadata. Use it only when ops explicitly require a legacy
variable name, and then document both names.

### When the starter must set Spring's own properties

Some starters must provide defaults for keys they do not own (`spring.datasource.*`,
`server.servlet.session.*`, `management.*`). Those cannot be expressed as `@ConfigurationProperties`.
Ship them as a defaults file loaded at **lowest precedence**, so the application and ops always win.

`src/main/resources/META-INF/drinkit/<name>-defaults.yml`:

```yaml
spring:
  datasource:
    hikari:
      auto-commit: false
```

```kotlin
package com.drinkit.notification.infra

import org.springframework.boot.EnvironmentPostProcessor   // Boot 4 package, not boot.env
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource

internal class NotificationDefaultsEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val resource = ClassPathResource("META-INF/drinkit/notification-defaults.yml")
        if (!resource.exists()) return

        // addLast → lowest precedence: application.yml, env vars and CLI args all override these
        YamlPropertySourceLoader()
            .load("notification-starter-defaults", resource)
            .forEach { environment.propertySources.addLast(it) }
    }
}
```

`src/main/resources/META-INF/spring.factories`:

```
org.springframework.boot.EnvironmentPostProcessor=\
com.drinkit.notification.infra.NotificationDefaultsEnvironmentPostProcessor
```

`spring.factories` is still the documented registration for `EnvironmentPostProcessor` in Spring
Boot 4.1 — unlike auto-configurations, which use the `.imports` file. Use the
`org.springframework.boot` package, not the deprecated `org.springframework.boot.env` one.

---

## 6. Several implementations, chosen by the consuming application

One property selects, the default is the safest implementation, and the app can always substitute its
own bean. Selection lives in the auto-configuration, not on the implementation classes.

```kotlin
@AutoConfiguration
@EnableConfigurationProperties(NotificationProperties::class)
internal class NotificationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "starters.notification", name = ["provider"], havingValue = "vendor")
    fun vendorNotificationSender(properties: NotificationProperties): NotificationSender =
        VendorNotificationSender(properties)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "starters.notification",
        name = ["provider"],
        havingValue = "default",
        matchIfMissing = true,
    )
    fun loggingNotificationSender(): NotificationSender = LoggingNotificationSender()
}
```

Rules: exactly one property key drives the choice; every branch is `@ConditionalOnMissingBean`; one
branch carries `matchIfMissing = true` so the context always starts; each branch is covered by an
`ApplicationContextRunner` test. Never use `@Profile` for this — `dev` is the application's business,
and a starter that needs `dev` to behave differently actually needs a property.

When the application must pick among several *instances* rather than one implementation (several
clients, several queues), expose a factory or a named-bean builder in the public API instead of
multiplying conditional beans.

---

## 7. Database access

Depend on `postgresql-starter`, never on jOOQ or the PostgreSQL driver directly. Add
`com.drinkit.jooq-codegen-convention` and scope generation to the starter's own tables:

```kotlin
jooq {
    executions.getByName("main") {
        configuration.apply {
            generator.apply {
                database.apply {
                    includes = "notification_outbox"
                    inputSchema = "drinkit_application"
                }
                target.apply {
                    packageName = "com.drinkit.notification.generated.jooq"
                }
            }
        }
    }
}
```

Generated sources are committed under `src/generated/jooq/kotlin` and regenerated on demand only
(`./gradlew :<name>-starter:jooqCodegen`, PostgreSQL running locally).

The repository implementation is `internal`, has no stereotype, and is registered by a `@Bean` method
in the auto-configuration. Transactions stay owned by the consumer's use case, so put
`@Transactional(propagation = Propagation.MANDATORY)` on the class:

```kotlin
@Transactional(propagation = Propagation.MANDATORY)
internal class JooqNotificationOutbox(private val dsl: DSLContext) : NotificationOutbox { … }
```

`@JooqRepository` from `postgresql-starter` expresses the same intent but is meta-annotated
`@Repository`, so a component scan would register a second instance. Prefer the plain `@Transactional`
above in a new starter, and keep `@JooqRepository` for business modules that are component-scanned on
purpose.

Integration tests use `@JooqIntegrationTest(schemas = [...])` from `postgresql-starter` — a
Testcontainers PostgreSQL per test class, schema created from the jOOQ DDL export, rollback after each
test. Do not write another one.

---

## 8. Test fixtures — the alternative to MockK

`testFixtures` exist so that every consumer tests against the *same* behaviour. A MockK stub is
configured per test and drifts; an in-memory implementation is written once and behaves like the real
thing everywhere. Ship at least one of:

- **In-memory** when the starter holds state — `InMemoryConfigurations` in `configuration-starter`.
- **Spy** when the starter only produces side effects — `SpyEmailSender` in `mail-starter`.
- **Programmable mock** when behaviour must be steered — `MockFeatureFlags` in `feature-flags-starter`
  (`configure(flag, enabled)` plus assertion helpers).

They are plain public classes, no Spring annotation, no I/O, no Testcontainers. They implement the
public interface, so the compiler breaks them when the API changes — that is the point.

```kotlin
package com.drinkit.notification

class SpyNotificationSender : NotificationSender {
    private var sent: List<Notification> = emptyList()

    override fun send(notification: Notification) { sent += notification }

    fun count(): Int = sent.size
    fun lastSent(): Notification? = sent.lastOrNull()
    fun sentTo(recipient: String): List<Notification> = sent.filter { it.recipient == recipient }
}
```

When consumers need real infrastructure, also ship a JUnit 5 extension plus a meta-annotation, like
`MeilisearchExtension` / `@MeilisearchIntegrationTest`: reusable container (`withReuse(true)`),
`ParameterResolver` to inject the client, cleanup in `afterAll`.

---

## 9. Tests inside the starter

Three layers, all required for a non-trivial starter.

**Wiring** — proves the auto-configuration contract without booting an application:

```kotlin
internal class NotificationAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(NotificationAutoConfiguration::class.java))

    @Test
    fun `registers the default sender`() {
        runner.run { context ->
            context.getBean(NotificationSender::class.java).shouldBeInstanceOf<LoggingNotificationSender>()
        }
    }

    @Test
    fun `backs off when the application declares its own bean`() {
        runner.withBean(NotificationSender::class.java, { SpyNotificationSender() })
            .run { context ->
                context.getBean(NotificationSender::class.java).shouldBeInstanceOf<SpyNotificationSender>()
            }
    }

    @Test
    fun `registers nothing when disabled`() {
        runner.withPropertyValues("starters.notification.enabled=false")
            .run { context -> context.containsBean("notificationSender") shouldBe false }
    }

    @Test
    fun `selects the vendor implementation`() {
        runner.withPropertyValues("starters.notification.provider=vendor")
            .run { context ->
                context.getBean(NotificationSender::class.java).shouldBeInstanceOf<VendorNotificationSender>()
            }
    }
}
```

**Behaviour** — an `internal abstract class XxxTestContract` holding the tests, one subclass per
implementation, exactly as `ConfigurationsTestContract` / `InMemoryConfigurationsTest` /
`JooqConfigurationsRepositoryTest` do. This is what guarantees the in-memory fixture and the real
implementation agree, and it is the reason the fixture can be trusted by consumers.

**Integration** — `@JooqIntegrationTest(schemas = [...])` or the starter's own extension.

Assertions with Kotest; deterministic time and randomness with `ControlledClock` / `ControlledRandom`
from `test-starter`. Inside the starter, MockK is acceptable for a third-party client with no sane
fake; it is never acceptable in what is shipped to consumers.

---

## 10. Documentation

`@TechStarterTool` (from `documentation-starter`, already on the classpath through
`library-convention`) goes on every public interface, annotation and entry-point class of the API.
KDoc on the interface and on each method — the KSP processor lifts both into the generated page.

`README.md` at the module root is the authoritative source: KSP prepends it verbatim to
`docs/src/engineering/resources/tech-starters/<name>-starter.md`, appends the generated API reference,
and adds the module to the overview page. Generate with `./gradlew :<name>-starter:kspKotlin` (it
never runs during a normal build) and commit the result.

Write it for a developer consuming the starter, never for its maintainer:

1. `# <Name> Starter` + one sentence on what it provides.
2. `## What is <concept>?` — 2–4 sentences plus concrete use cases.
3. `## Architecture Overview` — a `sequenceDiagram` for the main flow, a `graph LR` for component
   boundaries and, when there are several implementations, for the selection mechanism. Mermaid
   renders natively. Colours used across existing starters: `#e1f5e1` consumer code, `#e1e5ff` starter
   public API, `#fff3e1` internal implementation, `#ffe1e1` external system.
4. `## Key Features` — consumer-visible benefits, with snippets.
5. `## Configuration` — a table with property, default, **environment variable**, and whether it is
   mandatory in production.
6. `## Usage` — Gradle dependency, business-code example, test example with the fixture, integration
   test annotation.

Do not document internal class names, Spring wiring details, or maintenance instructions.

When the starter carries a non-obvious architectural concept, write the reference implementation as an
example under `src/test/kotlin` (`CarExample.kt` in `event-sourcing-starter`) and link to it from the
README rather than duplicating code that will rot.

---

## 11. Checklist before declaring it done

- [ ] `tech-starters/backend/<name>-starter/` with the layout of §2
- [ ] No dependency on `drinkit-domain` / `drinkit-infra` / `drinkit-backend`
- [ ] Public API minimal, `@TechStarterTool` + KDoc; everything else `internal`
- [ ] **Zero `@Service` / `@Component` / `@Repository` / `@Configuration` stereotype in `src/main`**
- [ ] One `@AutoConfiguration` class, listed in
      `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- [ ] `@Bean` methods carry no `internal` modifier
- [ ] Every bean `@ConditionalOnMissingBean`; starter switchable off by `starters.<name>.enabled`
- [ ] No Spring profile anywhere; nothing added to `spring.profiles.include`
- [ ] Properties under `starters.<name>.*`, constructor-bound, defaults in Kotlin, env var names documented
- [ ] `testFixtures` ship an in-memory/spy implementation; no MockK reaches consumers
- [ ] `ApplicationContextRunner` tests: default, override, disabled, each implementation branch
- [ ] Behaviour test contract shared between the fixture and the real implementation
- [ ] jOOQ: scoped `includes`, generated sources committed, `@JooqIntegrationTest` used
- [ ] `README.md` written for the consumer, with the configuration table and Mermaid diagrams
- [ ] `./gradlew :<name>-starter:kspKotlin` run and generated docs committed
- [ ] `./gradlew :<name>-starter:build` and `./gradlew :<name>-starter:detektAll` green
- [ ] Consumer wiring verified: the application gets the bean with only
      `implementation(project(":<name>-starter"))` added and no other change

---

## 12. Modernising an existing starter

The current starters predate these rules: component scan plus `spring.profiles.include` plus
`application-<name>.yml`. Migrate one at a time, and never mix the two mechanisms in one module:

1. Add the `@AutoConfiguration` class and the `.imports` file; move every `@Bean` into it.
2. Strip `@Service` / `@Component` / `@Configuration` from the implementation classes.
3. Move `application-<name>.yml` to `META-INF/drinkit/<name>-defaults.yml` behind an
   `EnvironmentPostProcessor` (§5), keeping the keys the application already relies on.
4. Remove the profile from `spring.profiles.include` in `application.yml` — last, and only once the
   `ApplicationContextRunner` tests and a `bootRun` both pass.
5. Rename properties into `starters.<name>.*`, keeping the old key bound for one release and marking it
   deprecated in the README.
