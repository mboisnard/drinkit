# Test Starter

A [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/tech-starters.md)
that puts the test libraries of DrinkIt on a module's test classpath, with a clock and a random generator that a
test controls. It has no Spring bean and no configuration.

## How a module gets it

No build file declares it. The `com.drinkit.test-convention` plugin of `build-logic`, which the base plugin of every
Kotlin module applies, adds it to `testImplementation`. The `com.drinkit.test-fixtures-convention` plugin adds it to
`testFixturesImplementation`, so the doubles and fixtures of `src/testFixtures` use it too.

## Libraries

The starter brings JUnit Jupiter, Spring Boot test, Kotest assertions, kotlin-test, Testcontainers, Kotlin Faker,
REST Assured, Cucumber and ApprovalTests. DrinkIt uses them as follows:

- The [architecture tests](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/writing-code/testing/architecture-tests.md)
  refuse AssertJ and Mockito, which `spring-boot-starter-test` brings, and the kotlin-test assertions: assertions go
  through Kotest and doubles are hand-written.
- Give Kotlin Faker a seed, as the domain's test fixtures do, so that its data repeats from one run to the next.
- The JUnit extensions of the starters' test fixtures, such as `@JooqIntegrationTest`, start the Testcontainers
  containers.
- `cucumber-junit` is the JUnit 4 runner, and no vintage engine is on the classpath: no Cucumber scenario runs.

## Controlled time

`ControlledClock` is a `java.time.Clock` that follows its delegate, the system clock by default, until a test fixes
it. The code under test receives it as its `Clock`:

```kotlin
val clock = ControlledClock().fix(Instant.parse("2026-11-03T10:15:30Z"))

clock.add(Duration.ofMinutes(16))
clock.instant() shouldBe Instant.parse("2026-11-03T10:31:30Z")
```

`fix()` freezes it at the current instant, and `reset()` makes it follow its delegate again. `add` moves a fixed
clock only, and throws `IllegalArgumentException` on one that is not fixed. A clock derived with `withZone` reads the
same controlled time. The clock can be a bean of a Spring test context that the test moves, as the tests of
`search-engine-starter` do.

## Controlled randomness

`ControlledRandom.value` is a `kotlin.random.Random` with a fixed seed: each run draws the same values.

```kotlin
val quantity = ControlledRandom.value.nextInt(1, 12)
```

It is one object for the whole test JVM, so the values a test draws depend on the tests that drew before it.
