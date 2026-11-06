# Tests on the backend

Test in the order of the `backend-feature` skill, whose `examples.md` names the model of each piece. What is
specific to the tests:

- A use case is tested through its `invoke`, with the in-memory doubles its `*Fixtures` class wires, as
  `CreateNewUserTest` does: one or two critical paths, what it loads and saves. Every decision, refusals included, is
  tested on its functional core only, in a `@Nested` class of that test.
- A new port gets its test contract in the test fixtures of the `-domain` module, run first against the in-memory
  double, as `InMemoryExchangeRatesTest` runs `ExchangeRatesTestContract`.
- The adapter runs the same contract against the real technology, in the application's `-infra` module, as
  `JooqExchangeRatesTest` does, or in the starter that owns the adapter.
