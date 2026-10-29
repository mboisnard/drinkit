# Models for a backend feature

Each model is named by class, as `AGENTS.md` says. Copy its shape, not its content.

## Domain types

- An entity: `Cellar`, annotated `@CoreDomain`.
- An identifier: `CellarId`, which extends `AbstractId` and checks itself in `init`. `AbstractIdJacksonModule`
  in the `-backend` module serializes it as a plain string, with nothing to add.
- An input value: `CellarName`, whose `validate()` returns its errors and never throws.

## Use case

`DeleteUser`, the reference command use case:

- a `*Command` data class annotated `@Command`;
- the use case: `@Service`, `@Usecase`, `@ImperativeShell`, a single `fun invoke` taking the id of what it acts on
  and the command, and a sealed `Result` nested in it. It is `@RetryableTransactional` because it appends to the
  `User` aggregate, as [user-events.md](user-events.md) says: a use case that saves a state is `@Transactional`;
- the core: an `internal object` named `<Action>Decider`, such as `UserDeletionDecider`, with a nested sealed
  `Decision`. It receives the date as `OffsetDateTime.now(clock)`, computed by the use case, and a new id the same
  way: `generateId.invoke(UserId::class)` in `CreateNewUser`.

A query: `FindExchangeRate`.

## Ports and their doubles

- A port of one context: `Cellars`, `UserEvents`. A port several contexts share: `GenerateId`. A port of a
  generic context the others call: `NotificationSender`.
- A contract test: `ExchangeRatesTestContract` in the domain test fixtures, run by `InMemoryExchangeRatesTest`
  against the in-memory double and by `JooqExchangeRatesTest` against PostgreSQL.
- The wiring of doubles and use cases: `UserFixtures`, which also wires `ControlledClock` from `test-starter`
  and `FakeGenerateId`.

## Adapters

- jOOQ: `JooqCellars`, the model of an adapter's name. It is annotated `@JooqRepository` from
  `postgresql-starter`, takes the `DSLContext`, maps records to domain types in private extension functions,
  and reads a JSONB column through `JSONBToJacksonConverter` with an injected `JsonMapper`.
- An HTTP client: `EuropeanCentralBankClient`, the `@GetExchange` interface, registered by
  `ForexClientsConfiguration`, its base URL under `spring.http.serviceclient.<group>`, and wrapped by
  `EuropeanCentralBankProvider`, the adapter. A secret is read through
  `Configurations`, as `ExchangeRateApiProvider` does.

## Controller and event handler

- A controller: `RegistrationController`, an `internal` `@RestController` that extends `AbstractApi` and
  implements the generated `<Tag>Api`. The connected user comes from `connectedUserIdOrFail()` of `AbstractApi`.
- A platform event handler: `RegistrationHandler`, which calls a use case.
