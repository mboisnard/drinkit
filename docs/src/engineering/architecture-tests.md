# Architecture Tests

DrinkIt's architecture rules are tests: breaking one fails `./gradlew build`, with the id of the rule, its reason and how to fix it. They live in `architecture-tests`, run by [Konture](https://github.com/baole/konture), which reads the Gradle module graph and the Kotlin sources of every module without compiling them. The choice of Konture is recorded in [ADR 0001](adr/0001-architecture-tests-with-konture).

```
./gradlew :architecture-tests:test
```

`./gradlew build` runs them, as CI does, and so does Claude Code's compile hook at the end of a turn that changed Kotlin sources, which hands a broken rule back to Claude.

## How a rule is written

One file per theme, one test per rule, named after the rule. The test holds the whole rule in Konture's DSL: its id, a description giving its reason and the fix, and the modules it reads, such as `that().inModule(DOMAINS)`. It declares a rule and checks it, so it has no `// Given`, `// When`, `// Then`. `Declarations.kt` adds what Konture lacks: the names a file or a class uses, what a module declares, and the roles DrinkIt gives its classes, port, adapter and controller.

The rules find the modules by a naming convention, never by their name or their folder, so that a new application or tech starter is checked from its first build, and a renamed or moved one stays checked. An application's modules start with its name, and its domain, infra and backend end in `-domain`, `-infra` and `-backend`: `drinkit-domain`, `drinkit-api-contract`. A package starts with a root of two segments, its organisation's reversed domain, then its bounded context: `com.drinkit`, then `cellar`. No rule names the root, so an application of another organisation is checked as well. Applications share nothing but the tech starters, the modules whose name ends in `-starter`. Any other module, such as `updater`, is out of the rules.

A new rule is shown failing once, on a violation written for the occasion, before it is trusted. A rule that selects nothing fails, so a renamed package cannot empty it. Generated code, the jOOQ classes and the OpenAPI delegates, is out of every rule, as long as its package holds a `generated` segment.

## Tolerated violations

The code written before a rule may break it. Each rule lists those violations in its own `suppress` block, by name, each with the reason it waits: the issue that removes it. A file is named by the path of its package, `*/com/drinkit/user/GenerateVerificationToken.kt`, which holds whatever its module or folder. A name never depends on a line, so editing the file around a violation keeps it tolerated. A name tolerates every violation of its rule by that class or file, not only the one it was written for, and a slice may also hide another cycle through the same packages. Once the violation is fixed, delete its line.

Konture's own baseline is not used: it records the line of each violation, so a line added above a tolerated one failed the build as a new violation.

## Not checked

Konture reads neither `build-logic`, nor the text of the build files, nor the resources. Rules on them, one archetype per module, versions only in the BOM, the `.imports` file of an auto-configuration, are left to review.

## Domain

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| The domain reaches infrastructure only through its ports | `domain.no-infrastructure-dependency` | The domain reaches databases, JSON, HTTP, the web, the Jakarta APIs and the infra modules only through a port of its spi package: move this code into an adapter of the infra module |
| The domain depends on no backend module | `domain.no-backend-dependency` | The backend drives the domain through its use cases, never the other way round: move what the domain needs from the backend into the domain |
| The domain uses Spring only for services, components and transactions | `domain.spring-only-for-services-components-and-transactions` | The domain stays free of Spring but for @Service, @Component and @Transactional, or a starter annotation built on them such as @RetryableTransactional: move this code into the infra or the backend module |
| A port is an interface in a spi package | `domain.ports-are-interfaces-in-spi` | A port is an interface in the spi package of its context, or of common, which the adapters implement: move this type there, or out of spi when it is not a port |
| A core domain type lives in a core package | `domain.core-domain-in-core` | A type annotated @CoreDomain is an entity or a value of its context: move it into the core package of that context |
| The core of a context never uses its use cases, commands or deciders | `domain.core-ignores-its-context-package` | Entities and values know nothing of the use cases that drive them, nor of what sits beside the use cases in the package of the context, commands and deciders: move the dependency into the use case |
| A use case is a service annotated as a use case | `domain.use-cases-are-annotated` | A use case is a @Service annotated @Usecase, which documents it: add the annotation |
| A command is a data class annotated as a command | `domain.commands-are-annotated-data-classes` | A command is the immutable input of a use case, documented as such: make it a data class annotated @Command |
| A decider is an internal object | `domain.decider-is-internal-object` | A decider holds no state and is called by its use case only: make it an internal object |
| A decider uses only its root package, Kotlin, java time and java math, never a port or a clock | `domain.decider-is-pure` | A decider decides from its arguments alone, and its use case does the I/O around it: it uses only the types of its root package, com.drinkit here, and those of Kotlin, java.time and java.math, never a port or a clock. Pass the values in |
| A domain name says what a thing is, never its pattern | `domain.names-say-what-not-pattern` | A domain name says what a thing is, never its pattern, so none ends in UseCase, Service, Repository, Impl or Dto: a port is Cellars, a use case CreateCellar. Rename it |
| The domain reads no system clock and draws no identifier or random value itself | `domain.time-and-ids-from-providers` | Time comes from the injected Clock, identifiers from GenerateId and randomness from a port, so tests control them, never from Clock.system(), now() without a clock, System.currentTimeMillis(), UUID.randomUUID() or Random: pass the clock to now(), and inject the others |

## Bounded contexts

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| Bounded contexts depend on each other without a cycle | `contexts.no-cycle-between-contexts` | A bounded context may use another, never the other way round as well: move what both need into the one that owns it, or into common |
| The packages of the domain depend on each other without a cycle | `contexts.no-cycle-between-packages` | The packages of a domain depend on each other in one direction only, so that each one reads and changes without the others: break the cycle by moving the type to the package the others depend on |
| Common depends on no bounded context | `contexts.common-depends-on-no-context` | common holds what every context shares, so it knows none of them: move the type into the context it depends on, or the dependency into the context |

## Infra

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| Every class of infra is internal | `infra.classes-are-internal` | Infra is reached through the ports only, which Kotlin enforces once its classes are internal: make the class internal |
| An adapter is named after its technology and its port | `infra.adapter-named-after-port` | An adapter's name starts with its technology or source and ends with the port it implements, or the last word of that port: JooqCellars for Cellars, EuropeanCentralBankProvider for ExchangeRateProvider. Rename it |
| A jOOQ adapter is a jOOQ repository | `infra.jooq-adapter-is-jooq-repository` | A jOOQ adapter is a @JooqRepository, which demands a running transaction and translates the database errors: annotate it |
| jOOQ appears only in infra and in starters | `infra.jooq-only-in-infra-and-starters` | SQL lives in the adapters of the infra modules and in the starters that own a table: move this code behind a port |
| An adapter sits in the package of its port's context | `infra.adapter-in-context-of-its-port` | An adapter sits in the package of the context its port belongs to, so a context reads as one package across the modules: move it there |
| An adapter uses no service of the domain | `infra.adapter-uses-no-domain-service` | The domain drives the adapters, never the other way round: an adapter uses no @Service or @Component of the domain, use case or not. Move the call into the use case that calls the port |

## Backend

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| An endpoint is an internal component implementing a delegate generated from the contract | `backend.endpoints-implement-generated-delegates` | The API contract defines every endpoint and generates its delegate: implement it as an internal @Component named after it, CellarsApi for CellarsApiDelegate |
| No endpoint of the backend is mapped by hand | `backend.no-endpoint-mapped-by-hand` | An endpoint the contract does not define escapes its review and the generated client: add it to the API contract of the application and implement its delegate |
| The backend uses no port directly | `backend.calls-use-cases-never-ports` | The backend turns a request or a message into a call to a use case, which holds the rules and the transaction around the ports: call a use case instead of the port |
| A controller package is named after its bounded context | `backend.controller-package-named-after-context` | A controller sits in the package of the bounded context it serves, so a context reads as one package across the modules: move it to the package of its context, such as com.drinkit.cellar |
| The backend uses no class or function of infra | `backend.uses-nothing-of-infra` | The backend reaches infra through the use cases only, which call the ports Spring wires the adapters behind: call a use case instead of the adapter or its function |

## Spring

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| Beans are injected through their constructor | `spring.constructor-injection-only` | A bean receives its dependencies through its constructor, never through @Autowired, @Inject or @Resource, so it is complete once built and testable without Spring: replace the injected field with a constructor parameter |
| A settings class is internal and its name ends in Properties | `spring.settings-classes-internal-and-named` | Settings bind to one class per group, internal to its module and named *Properties: rename it, or make it internal |
| Settings are never read one by one | `spring.no-value-or-environment-lookup` | A setting read with @Value or from the Environment escapes validation and documentation: bind it to a @ConfigurationProperties class |
| A configuration class is named after its configuration | `spring.configuration-named-configuration` | A @Configuration class says so in its name, so the wiring of a module is found at a glance: rename it *Configuration |
| A class never calls its own proxied method | `spring.no-self-invocation-of-proxied-method` | A call from the same object skips the proxy, so the transaction, retry, cache or async of the method, or of its class, never applies: call the method from another bean, or move the annotation to the caller |
| A proxied method is visible to its proxy | `spring.proxied-method-is-visible` | A proxy only intercepts the methods it can override from outside: make the annotated method public or internal, which Kotlin compiles to public |

## Tech starters

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| A tech starter depends on no application | `starters.depend-on-no-application` | A tech starter holds one technical concern, reusable without the business code: replace the dependency on an application module with a port the application implements |
| The package of a tech starter is named after the module | `starters.package-named-after-module` | A tech starter is found by its name: its code, test fixtures included, lives in the package named after the module without -starter, its hyphens dropped or turned into dots, under the root package: com.drinkit.event.sourcing for event-sourcing-starter. Rename the package or the module |
| No package of a tech starter names a bounded context | `starters.serve-no-bounded-context` | A tech starter holds technical code any application may use, so none of its packages names a bounded context: move the business code into the application |
| Only the public API of a tech starter is public, and it says so | `starters.public-api-is-marked` | A tech starter shows only its public API, marked @TechStarterTool so that its documentation lists it, and keeps the rest internal: mark the class, or make it internal |
| A tech starter wires its beans without stereotype annotations | `starters.no-stereotype` | A tech starter never makes a bean by annotating its class, which only a scan of the starter's packages finds: declare the bean in the starter's @AutoConfiguration |
| A bean of a tech starter backs off when the application defines its own | `starters.beans-back-off` | A bean of a tech starter gives way to the application's own: annotate it @ConditionalOnMissingBean |
| The settings of a tech starter live under starters | `starters.settings-under-starters-prefix` | The settings of a tech starter live under `starters.<name>`, so they never clash with Spring's or the application's: change the prefix |

## Tests

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| Tests use no mocking library | `tests.no-mocking-library` | A test runs against hand-written doubles of src/testFixtures, never mocks, so it checks behavior rather than calls: write an in-memory, spy or fake double instead |
| Tests assert with Kotest only | `tests.kotest-assertions-only` | Tests assert with Kotest only, never AssertJ, Hamcrest, kotlin.test or JUnit's assertions, so failures read the same everywhere: replace the assertion with its Kotest matcher |
| A test is named by a sentence in backticks | `tests.backticked-names` | A test name is a sentence in backticks stating the behavior, so the test names read as the specification: rename it |
| No test is disabled | `tests.no-disabled-test` | A disabled test checks nothing while it looks covered: fix it, or delete it |
| A port has a test contract that its double and its adapter both pass | `tests.port-has-test-contract` | A port's behavior is written once, in an abstract `<Port>TestContract` beside the port in the domain test fixtures, which its in-memory double and its real adapter both extend: write it, or extend it |
| A double lives in the test fixtures and is named after its kind | `tests.doubles-named-after-their-kind` | A double of a port is shared from src/testFixtures and says what it is: InMemory, Spy or Fake, never Mock, since nothing is mocked. Move or rename it |
| A jOOQ adapter is tested against a real database | `tests.jooq-adapter-test-is-integration-test` | A jOOQ adapter is tested against PostgreSQL, through @JooqIntegrationTest and the committed jOOQ schema: annotate the test |
| Tests build their data through the domain | `tests.no-sql-data-setup` | A test builds its data through the domain and its fixtures, which follow every schema change, never through SQL scripts: replace @Sql with fixtures |

## Everywhere

| Rule | Id | Why, and how to fix a violation |
|---|---|---|
| An application uses no module of another application | `codebase.applications-are-independent` | An application depends on no module of another application, so that each one builds, tests and ships alone: reach the other application through its API or its messages, or move the technical code into a tech starter |
| An application calls HTTP through registered declarative clients | `codebase.http-through-registered-clients` | An application never builds an HTTP request by hand with RestTemplate, RestClient, WebClient or java.net.http, which only a tech starter does: declare an @HttpExchange interface and register it with @ImportHttpServices |
| No code uses Date or Calendar | `codebase.no-legacy-date` | Dates are java.time types, which carry their zone and stay immutable: replace Date or Calendar with Instant, OffsetDateTime or LocalDate |
| A logger is a private KotlinLogging property named logger | `codebase.logger-convention` | A logger is declared one way, private val logger = KotlinLogging.logger {}, so it reads the same in every file, and never comes from the getLogger() of SLF4J, Log4j or java.util.logging: rename it, make it private, or create it with KotlinLogging |
