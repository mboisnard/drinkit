# Architecture Tests

DrinkIt's architecture rules are tests: breaking one fails `./gradlew build`, with the id of the rule, its reason and how to fix it. They live in `architecture-tests`, run by [Konture](https://github.com/baole/konture), which reads the Gradle module graph and the Kotlin sources of every module without compiling them. The choice of Konture is recorded in [ADR 0001](../../adr/0001-architecture-tests-with-konture).

```
./gradlew :architecture-tests:test
```

`./gradlew build` runs them, as CI does, and so does Claude Code's compile hook at the end of a turn that changed Kotlin sources, which hands a broken rule back to Claude.

## How a rule is written

One file per theme, one test per rule, named after the rule. Within a file, and a section of this page, the rules go from what the code may use, to its shape, to its names. The test holds the whole rule in Konture's DSL: its id, a description giving its reason and the fix, and the modules it reads, such as `that().inModule(DOMAINS)`. It declares a rule and checks it, so it has no `// Given`, `// When`, `// Then`. `Declarations.kt` holds every helper the rules share, each with a comment: one starting "Konture lacks" names the gap of Konture the helper works around, the others a DrinkIt convention, such as how a module, a bounded context, a port or an adapter is found.

The rules find the modules by a naming convention, never by their name or their folder, so that a new application or tech starter is checked from its first build, and a renamed or moved one stays checked. An application's modules start with its name, and its domain, infra and backend end in `-domain`, `-infra` and `-backend`: `drinkit-domain`, `drinkit-api-contract`. The root package of an application is the one of the `@SpringBootApplication` of its backend, and a bounded context is the package right under it: `com.drinkit`, then `cellar`. A context holds its use cases, with the commands and deciders beside them, at its root, its entities and values in `core` and its ports in `spi`, while `common` holds what the contexts share. A tech starter's root is the part of its package it shares with an application root: `com.drinkit` for `com.drinkit.security`. No rule names the root, so an application of another organisation, or rooted deeper such as `com.drinkit.billing`, is checked as well. A backend with no `@SpringBootApplication` fails the build, since its application has no root to read. Applications share nothing but the tech starters, the modules whose name ends in `-starter`. Any other module, such as `updater` or a shared library, is out of the rules.

A new rule is shown failing once, on a violation written for the occasion, before it is trusted. A rule that selects nothing fails, so a renamed package cannot empty it. Generated code, the jOOQ classes and the OpenAPI interfaces, is out of every rule, as long as its package holds a `generated` segment.

## Tolerated violations

The code written before a rule may break it. Each rule lists those violations in its own `suppress` block, by name, each with the reason it waits: the issue that removes it. A file is named by the path of its package, `*/com/drinkit/user/GenerateVerificationToken.kt`, which holds whatever its module or folder. A name never depends on a line, so editing the file around a violation keeps it tolerated. A name tolerates every violation of its rule by that class or file, not only the one it was written for, and a slice may also hide another cycle through the same packages. Once the violation is fixed, delete its line.

Konture's own baseline is not used: it records the line of each violation, so a line added above a tolerated one failed the build as a new violation.

## Not checked

Konture reads neither `build-logic`, nor the text of the build files, nor the resources. Rules on them, one archetype per module, versions only in the BOM, the `.imports` file of an auto-configuration, are left to review. The libraries a file uses are read from its imports: one used by its full name, with no import, is not seen.

## Domain

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| The domain depends on no infra module | `domain.no-infra-dependency` | The domain reaches infra through the ports of its `spi` package only: remove the dependency on the `-infra` module and call a port |
| The domain depends on no backend module | `domain.no-backend-dependency` | The backend drives the domain, never the reverse: move what the domain needs from the backend into the domain |
| The domain depends only on the starters that carry no infrastructure | `domain.depends-only-on-library-starters` | A starter that wires a database, the web or mail is infrastructure: the domain depends only on the starters of `DOMAIN_STARTERS`, which grows by an architecture decision |
| The domain uses only its own code, its starters, Kotlin, the JDK and Spring's services and transactions | `domain.uses-only-allowed-code` | The domain imports only its own code and starters, Kotlin, `java.lang`, `java.time`, `java.math`, `java.util`, `KotlinLogging` and Spring's `@Service`, `@Component` and `@Transactional`: move any other library behind a port |
| The core of a context uses only core and common types | `domain.core-depends-on-core-only` | Entities and values never use a use case, a command, a decider or a port: move the dependency into the use case, or the type into `core` |
| A port speaks only core and common types | `domain.spi-depends-on-core-only` | A port speaks the `core` and `common` types, never those of the use cases that call it: move the type it needs into `core` |
| A decider uses only its root package, Kotlin, java time and java math, never a port or a clock | `domain.decider-is-pure` | A decider decides from its arguments alone: it uses only the types of its application, Kotlin, `java.time` and `java.math`, never a port or a clock. Pass the values in |
| The domain reads no system clock and draws no identifier or random value itself | `domain.time-and-ids-from-providers` | Tests control time, identifiers and randomness only when they are injected: call `now(clock)` and `GenerateId`, never `Clock.system*()`, `now()`, `currentTimeMillis()`, `UUID.randomUUID()` or `Random` |
| A port is an interface in a spi package | `domain.ports-are-interfaces-in-spi` | A port is an interface in the `spi` package of its context, or of `common`: move it there, or out of `spi` when it is not a port |
| A core domain type lives in a core package | `domain.core-domain-in-core` | A `@CoreDomain` type is an entity or a value: move it into the `core` package of its context |
| A use case is a service annotated as a use case | `domain.use-cases-are-annotated` | The documentation lists the use cases by their `@Usecase`: annotate the `@Service` |
| A command is a data class annotated as a command | `domain.commands-are-annotated-data-classes` | A command is the immutable and documented input of a use case: make it a data class annotated `@Command` |
| A decider is an internal object | `domain.decider-is-internal-object` | A decider holds no state and only its use case calls it: make it an internal object |
| A domain name says what a thing is, never its pattern | `domain.names-say-what-not-pattern` | A domain name says what a thing is, not its pattern, such as `Cellars` or `CreateCellar`: drop the `UseCase`, `Service`, `Repository`, `Impl` or `Dto` suffix |

## Bounded contexts

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| Bounded contexts depend on each other without a cycle | `contexts.no-cycle-between-contexts` | Two contexts never depend on each other both ways: move what both need into the one that owns it, or into `common` |
| The packages of the domain depend on each other without a cycle | `contexts.no-cycle-between-packages` | The packages of a domain depend on each other one way only: move the type that closes the cycle into the package the others use |
| Common depends on no bounded context | `contexts.common-depends-on-no-context` | Every context uses `common`, so `common` uses none of them: move the type into the context it depends on |

## Infra

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| jOOQ appears only in infra and in starters | `infra.jooq-only-in-infra-and-starters` | SQL lives in the infra adapters and in the starters that own a table: move this jOOQ code behind a port |
| An adapter uses the core and spi types of the domain only | `infra.uses-domain-through-core-and-spi` | The domain drives the adapters, never the reverse: an adapter uses only `core`, `spi` and `common` types. Move the call into the use case, or the type into `core` |
| Every class of infra is internal | `infra.classes-are-internal` | Infra is reached through the ports only: make the class internal |
| A jOOQ adapter is a jOOQ repository | `infra.jooq-adapter-is-jooq-repository` | `@JooqRepository` demands a transaction and translates the database errors: annotate the jOOQ adapter with it |
| An adapter sits in the package of its port's context | `infra.adapter-in-context-of-its-port` | A context reads as one package across the modules: move the adapter into the package of its port's context |
| An adapter is named after its technology and its port | `infra.adapter-named-after-port` | An adapter is named after its technology, then its port or the port's last word: `JooqCellars` for `Cellars`, `EuropeanCentralBankProvider` for `ExchangeRateProvider` |

## Backend

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| A backend class that uses a port needing a transaction runs in one | `backend.port-used-in-transaction` | A port whose adapter is a `@JooqRepository` needs a running transaction: annotate the class that uses it, or each of its non-private methods, `@Transactional` or `@RetryableTransactional` |
| A controller is internal and implements an interface generated from the contract | `backend.controllers-implement-generated-interfaces` | The contract generates each endpoint's interface: implement it in an internal `@RestController` named after it, `CellarsController` for `CellarsApi` |
| No endpoint of the backend is mapped by hand | `backend.no-endpoint-mapped-by-hand` | An endpoint outside the contract escapes its review and the generated client: add it to the contract and implement the generated interface |
| A controller package is named after its bounded context | `backend.controller-package-named-after-context` | A context reads as one package across the modules: move the controller into the package of its context, such as `com.drinkit.cellar` |

## Spring

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| Settings are never read one by one | `spring.no-value-or-environment-lookup` | A setting read with `@Value` or from the `Environment` escapes validation and documentation: bind it to a `@ConfigurationProperties` class |
| A class never calls its own proxied method | `spring.no-self-invocation-of-proxied-method` | A call on this skips the proxy, so `@Transactional`, `@Cacheable`, `@Async` or a retry never applies: call the method from another bean |
| Beans are injected through their constructor | `spring.constructor-injection-only` | A bean is complete once built and testable without Spring: replace the `@Autowired`, `@Inject` or `@Resource` field with a constructor parameter |
| A proxied method is visible to its proxy | `spring.proxied-method-is-visible` | A proxy intercepts only the methods it can override: make the annotated method public or internal |
| A settings class is internal and its name ends in Properties | `spring.settings-classes-internal-and-named` | A `@ConfigurationProperties` class belongs to its module and says what it binds: make it internal and name it `*Properties` |
| A configuration class of an application is internal | `spring.configuration-classes-internal` | The `@Configuration` of an application wires its own module only: make it internal. A tech starter's follows `starters.public-api-is-marked` |
| A configuration class is named after its configuration | `spring.configuration-named-configuration` | The wiring of a module is found by its name: rename the `@Configuration` class `*Configuration` |

## Tech starters

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| A tech starter depends on no application | `starters.depend-on-no-application` | A tech starter works without business code: replace the dependency on an application module with a port that the application implements |
| A tech starter reads no table of a bounded context | `starters.read-no-business-table` | A starter that reads a table of a context, such as `USER`, ties itself to the schema of one application: ask the application for the data through a port |
| Only the public API of a tech starter is public, and it says so | `starters.public-api-is-marked` | A tech starter documents its public API with `@TechStarterTool` and keeps the rest internal: mark the class, or make it internal |
| A tech starter wires its beans without stereotype annotations | `starters.no-stereotype` | Only a scan of the starter's packages finds a `@Component` or `@Service`: declare the bean in the starter's `@AutoConfiguration` |
| A bean of a tech starter backs off when the application defines its own | `starters.beans-back-off` | The application's own bean wins over a starter's: annotate the `@Bean` `@ConditionalOnMissingBean` |
| The settings of a tech starter live under starters | `starters.settings-under-starters-prefix` | Settings of a tech starter never clash with Spring's or the application's: put them under `starters.<name>` |
| The package of a tech starter is named after the module | `starters.package-named-after-module` | A tech starter's code, test fixtures included, lives in the package named after its module, `com.drinkit.event.sourcing` for `event-sourcing-starter`: rename the package or the module |

## Tests

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| Tests use no mocking library | `tests.no-mocking-library` | A test checks behavior through hand-written doubles, not mocks: replace `Mockito` or `MockK` with an `InMemory`, `Spy` or `Fake` double of `src/testFixtures` |
| Tests assert with Kotest only | `tests.kotest-assertions-only` | Failures read the same in every test: replace the `AssertJ`, `Hamcrest`, `kotlin.test` or JUnit assertion with its Kotest matcher |
| Tests build their data through the domain | `tests.no-sql-data-setup` | An SQL script drifts from the schema: replace `@Sql` with data built through the domain and its fixtures |
| A port has a test contract that its double and its adapter both pass | `tests.port-has-test-contract` | A port's behavior is written once, in an abstract `<Port>TestContract` of the domain test fixtures: write it, and extend it from the in-memory double's test and the adapter's test |
| A jOOQ adapter is tested against a real database | `tests.jooq-adapter-test-is-integration-test` | A jOOQ adapter is tested against PostgreSQL: annotate its test `@JooqIntegrationTest` |
| No test is disabled | `tests.no-disabled-test` | A disabled test looks covered but checks nothing: fix it, or delete it |
| A test class is internal | `tests.classes-are-internal` | A test class serves its own module only: make it internal or private. What other modules' tests share goes in `src/testFixtures` |
| A double lives in the test fixtures and is named after its kind | `tests.doubles-named-after-their-kind` | A double of a port lives in `src/testFixtures` and says what it is: name it `InMemory`, `Spy` or `Fake`, never `Mock` |
| A test is named by a sentence in backticks | `tests.backticked-names` | Test names read as the specification: name the test by a sentence in backticks |

## Everywhere

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| An application uses no module of another application | `codebase.applications-are-independent` | Each application builds and ships alone: reach another one through its API or its messages, or move the shared technical code into a tech starter |
| A package under the root belongs to one application or to the tech starters | `codebase.package-has-one-owner` | A package right under the root belongs to one application or to the tech starters, never to two: rename the context, or the package of the starter |
| An application calls HTTP through registered declarative clients | `codebase.http-through-registered-clients` | Only a tech starter builds HTTP requests by hand: replace `RestTemplate`, `RestClient`, `WebClient` or `java.net.http` with an `@HttpExchange` interface registered by `@ImportHttpServices` |
| No code uses Date or Calendar | `codebase.no-legacy-date` | `Date` and `Calendar` are mutable and carry no zone: use `Instant`, `OffsetDateTime` or `LocalDate` |
| A class declares no operator invoke | `codebase.no-operator-invoke-in-class` | A call such as `createCellar(command)` hides the function it runs: drop `operator` from the `invoke` of a class and call `invoke` by its name. An object, and the other operators, may keep it |
| A logger is a private KotlinLogging property named logger | `codebase.logger-convention` | Every file declares its logger the same way: `private val logger = KotlinLogging.logger {}`, never a logger of SLF4J, Log4j or `java.util.logging` |
