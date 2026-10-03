# Tests on the backend

Start from the domain and move out. The `hexagonal-backend` skill says where each piece goes, and its
`examples.md` names the model of each piece and of its test.

1. The use case, through its `invoke`, with the in-memory doubles its `*Fixtures` class wires, as
   `CreateNewUserTest` does. Its functional core is tested in a `@Nested` class.
2. A new port: its test contract, run first against the in-memory double.
3. The adapter: the same contract, run against the real technology in `drinkit-infra`.
4. The controller last, once the use case does what it should.
