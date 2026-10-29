# Tech starter examples

Shapes for the `new-backend-tech-starter` skill, written for an imaginary `sms-starter`. Rename
everything, keep only what the starter needs, and check every import against the code and the jars.

- [Module layout](#module-layout) and [build.gradle.kts](#buildgradlekts)
- [Auto-configuration](#auto-configuration), [Configuration properties](#configuration-properties) and
  [Defaults for Spring's own properties](#defaults-for-springs-own-properties)
- [Several implementations](#several-implementations) and [Repository](#repository)
- [Test fixtures](#test-fixtures) and [Wiring test](#wiring-test)
- [README](#readme)

## Module layout

```
tech-starters/backend/<name>-starter/
├── build.gradle.kts
├── README.md                                   ← source of the generated documentation page
└── src/
    ├── main/
    │   ├── kotlin/com/drinkit/<domain>/
    │   │   ├── XxxService.kt                   ← public API, @TechStarterTool
    │   │   ├── XxxTypes.kt                     ← public value types used by the API
    │   │   └── infra/
    │   │       ├── XxxAutoConfiguration.kt     ← internal, the only wiring entry point
    │   │       ├── XxxProperties.kt            ← internal @ConfigurationProperties
    │   │       └── DefaultXxxService.kt        ← internal implementation, no stereotype
    │   └── resources/META-INF/spring/
    │       └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
    ├── test/kotlin/com/drinkit/<domain>/
    │   ├── XxxAutoConfigurationTest.kt         ← ApplicationContextRunner, the wiring
    │   ├── XxxTestContract.kt                  ← behaviour shared by every implementation
    │   └── DefaultXxxTest.kt / JooqXxxTest.kt
    └── testFixtures/kotlin/com/drinkit/<domain>/
        ├── InMemoryXxx.kt or SpyXxx.kt         ← the double consumers' tests use
        └── XxxIntegrationTest.kt               ← meta-annotation of a JUnit extension, if Testcontainers
```

The Gradle project path is flat: `:<name>-starter`.

## build.gradle.kts

| Need | Plugin |
|---|---|
| Spring beans, the normal case | `com.drinkit.library-convention` |
| No Spring, pure utilities | `com.drinkit.common-convention`, and `documentation-convention` for the docs |
| Fixtures for consumers' tests | add `com.drinkit.test-fixtures-convention` |
| PostgreSQL tables | add `com.drinkit.jooq-codegen-convention` |

`common-convention` applies `test-convention`, which puts `test-starter` on the test classpath:
`ApplicationContextRunner`, Kotest and JUnit need no extra dependency.

```kotlin
plugins {
    id("com.drinkit.library-convention")
    id("com.drinkit.test-fixtures-convention")
    id("com.drinkit.jooq-codegen-convention")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    api(libs.some.client)
    compileOnly("org.springframework.boot:spring-boot-starter-actuator")

    testFixturesApi(libs.testcontainers.something)
}
```

- `spring-boot-autoconfigure` is what `@AutoConfiguration` and the `@ConditionalOn*` annotations need.
- `api` for a type that appears in the public API, `implementation` for the rest, `compileOnly` for an optional
  integration guarded by `@ConditionalOnClass`, `testFixturesApi` for what a consumer's test classpath needs.

## Auto-configuration

`src/main/kotlin/com/drinkit/sms/infra/SmsAutoConfiguration.kt`:

```kotlin
package com.drinkit.sms.infra

import com.drinkit.sms.SmsSender
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@ConditionalOnClass(SomeVendorClient::class)
@ConditionalOnProperty(prefix = "starters.sms", name = ["enabled"], matchIfMissing = true)
@EnableConfigurationProperties(SmsProperties::class)
internal class SmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun smsSender(properties: SmsProperties): SmsSender =
        DefaultSmsSender(properties.endpoint, properties.timeout)
}
```

`src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.drinkit.sms.infra.SmsAutoConfiguration
```

An optional integration, such as an actuator health indicator or a servlet filter, goes in a nested
`@Configuration(proxyBeanMethods = false)` class guarded by `@ConditionalOnClass` or
`@ConditionalOnWebApplication`, with its dependency declared `compileOnly`.

## Configuration properties

```kotlin
package com.drinkit.sms.infra

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "starters.sms")
internal data class SmsProperties(
    val enabled: Boolean = true,
    val endpoint: String = "",
    val timeout: Duration = Duration.ofSeconds(5),
    val provider: Provider = Provider.DEFAULT,
) {
    enum class Provider { DEFAULT, VENDOR }
}
```

## Defaults for Spring's own properties

`src/main/resources/META-INF/drinkit/sms-defaults.yml`:

```yaml
spring:
  datasource:
    hikari:
      auto-commit: false
```

```kotlin
package com.drinkit.sms.infra

import org.springframework.boot.EnvironmentPostProcessor
import org.springframework.boot.SpringApplication
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.ClassPathResource

internal class SmsDefaultsEnvironmentPostProcessor : EnvironmentPostProcessor {

    override fun postProcessEnvironment(environment: ConfigurableEnvironment, application: SpringApplication) {
        val resource = ClassPathResource("META-INF/drinkit/sms-defaults.yml")
        if (!resource.exists()) return

        // addLast gives the lowest precedence: application.yml, env vars and CLI args all override these
        YamlPropertySourceLoader()
            .load("sms-starter-defaults", resource)
            .forEach { environment.propertySources.addLast(it) }
    }
}
```

`src/main/resources/META-INF/spring.factories`:

```
org.springframework.boot.EnvironmentPostProcessor=\
com.drinkit.sms.infra.SmsDefaultsEnvironmentPostProcessor
```

## Several implementations

Exactly one property drives the choice, every branch is `@ConditionalOnMissingBean`, and each branch has its
`ApplicationContextRunner` test. When the application picks among several instances rather than one
implementation, such as several clients, the public API exposes a factory instead of more conditional beans.

```kotlin
@AutoConfiguration
@EnableConfigurationProperties(SmsProperties::class)
internal class SmsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "starters.sms", name = ["provider"], havingValue = "vendor")
    fun vendorSmsSender(properties: SmsProperties): SmsSender =
        VendorSmsSender(properties)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(
        prefix = "starters.sms",
        name = ["provider"],
        havingValue = "default",
        matchIfMissing = true,
    )
    fun loggingSmsSender(): SmsSender = LoggingSmsSender()
}
```

## Repository

```kotlin
@Transactional(propagation = Propagation.MANDATORY)
internal class JooqSmsOutbox(private val dsl: DSLContext) : SmsOutbox { … }
```

## Test fixtures

- In-memory, when the starter holds state: `InMemoryConfigurations` in `configuration-starter`.
- Spy, when it only produces side effects: `SpyEmailSender` in `mail-starter`.
- Programmable, when a test steers its behaviour: `InMemoryFeatureFlags` in `feature-flags-starter`, written by hand
  like the others.
- A JUnit extension and its meta-annotation, when consumers need the real infrastructure:
  `MeilisearchExtension` and `@MeilisearchIntegrationTest` in `search-engine-starter`.

The behaviour contract: `ConfigurationsTestContract`, run by `InMemoryConfigurationsTest` and
`JooqConfigurationsRepositoryTest`.

## Wiring test

```kotlin
internal class SmsAutoConfigurationTest {

    private val runner = ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SmsAutoConfiguration::class.java))

    @Test
    fun `registers the default sender`() {
        runner.run { context ->
            context.getBean(SmsSender::class.java).shouldBeInstanceOf<LoggingSmsSender>()
        }
    }

    @Test
    fun `backs off when the application declares its own bean`() {
        runner.withBean(SmsSender::class.java, { SpySmsSender() })
            .run { context ->
                context.getBean(SmsSender::class.java).shouldBeInstanceOf<SpySmsSender>()
            }
    }

    @Test
    fun `registers nothing when disabled`() {
        runner.withPropertyValues("starters.sms.enabled=false")
            .run { context -> context.containsBean("smsSender") shouldBe false }
    }

    @Test
    fun `selects the vendor implementation`() {
        runner.withPropertyValues("starters.sms.provider=vendor")
            .run { context ->
                context.getBean(SmsSender::class.java).shouldBeInstanceOf<VendorSmsSender>()
            }
    }
}
```

## README

Written for a developer who consumes the starter, never for its maintainer: no internal class names, no wiring
details.

1. `# <Name> Starter` and one sentence on what it provides.
2. `## What is <concept>?`: a few sentences and concrete use cases.
3. `## Architecture Overview`: a Mermaid `sequenceDiagram` for the main flow, a `graph LR` for the component
   boundaries and, with several implementations, for the selection. The existing starters colour consumer code
   `#e1f5e1`, the public API `#e1e5ff`, the internal implementation `#fff3e1` and an external system `#ffe1e1`.
4. `## Key Features`: what the consumer gains, with snippets.
5. `## Configuration`: a table of each property, its default, its environment variable, and whether production
   must set it.
6. `## Usage`: the Gradle dependency, a business-code example, a test with the fixture, the integration test
   annotation.
