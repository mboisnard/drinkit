# Models for a backend feature

Each model is named by class, as `AGENTS.md` says. Copy its shape, not its content.

## Domain types

- An entity: `Cellar`, annotated `@CoreDomain`.
- An identifier: `CellarId`, which extends `AbstractId` and checks itself in `init`. `AbstractIdJacksonModule`
  in `drinkit-backend` serializes it as a plain string, with nothing to add.
- An input value: `CellarName`, whose `validate()` returns its errors and never throws.

## Use case

`CreateNewUser`, the reference command use case:

- a `*Command` data class annotated `@Command`;
- the use case: `@Service`, `@Transactional`, `@Usecase`, `@ImperativeShell`, a single `fun invoke(command)` and
  a sealed `Result` nested in it;
- the core: an `internal object` named `<Action>Decider`, such as `UserCreationDecider`, with a nested sealed
  `Decision`. It receives the date as `OffsetDateTime.now(clock)` and new ids as
  `generateId.invoke(CellarId::class)`, computed by the use case.

A query: `FindExchangeRate`.

## Ports and their doubles

- A port of one context: `Cellars`, `UserEvents`. A port several contexts share, in `common`: `GenerateId`,
  `MessageSender`.
- A contract test: `ExchangeRatesTestContract` in the domain test fixtures, run by `InMemoryExchangeRatesTest`
  against the in-memory double and by `JooqExchangeRatesRepositoryTest` against PostgreSQL.
- The wiring of doubles and use cases: `UserFixtures`, which also wires `ControlledClock` from `test-starter`
  and `MockGenerateId`.

## Adapters

- jOOQ: `JooqCellars`, the model of an adapter's name. It is annotated `@JooqRepository` from
  `postgresql-starter`, takes the `DSLContext`, maps records to domain types in private extension functions,
  and reads a JSONB column through `JSONBToJacksonConverter` with an injected `JsonMapper`.
- An HTTP client: `EuropeanCentralBankClient`, the `@GetExchange` interface, registered by
  `ForexClientsConfiguration` and wrapped by `EuropeanCentralBankProvider`, the adapter. A secret is read through
  `Configurations`, as `ExchangeRateApiProvider` does.

## Controller and event handler

- A controller: `RegistrationApi`, an `internal` `@Component` that extends `AbstractApi` and implements the
  generated `<Tag>ApiDelegate`. The connected user comes from `connectedUserIdOrFail()` of `AbstractApi`.
- A platform event handler: `RegistrationHandler`, which calls a use case.
