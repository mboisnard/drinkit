# Backend test shapes

Shapes taken from DrinkIt's own tests, for Kotlin code. Find each named class with
`git grep -nwE '(class|interface|object) <Name>'`.

## A test

One behavior, named in backticks as a sentence, built from the doubles a `*Fixtures` class wires, and read in
three blocks. Model: `CreateNewUserTest`.

```kotlin
internal class FindExchangeRateTest {

    private val exchangeRateFixtures = ExchangeRateFixtures()
    private val findExchangeRate = exchangeRateFixtures.findExchangeRate

    @Test
    fun `find direct rate when it exists`() {
        // Given
        val usdToGbp = exchangeRateFixtures.givenAnExchangeRate(USD, GBP, BigDecimal("0.80"))

        // When
        val rate = findExchangeRate.invoke(USD, GBP)

        // Then
        rate shouldBe usdToGbp
    }
}
```

State comes from the fixtures' `given…` functions, which go through the same code as production, rather than from
objects built field by field in the test.

## Assertions with Kotest

| What the code returns | Assert it with |
|---|---|
| A value that may be null | `rate.shouldNotBeNull() should { it.target shouldBe GBP }`, never `!!`. Its absence: `result.shouldBeNull()` |
| A case of a sealed result | `result.shouldBeInstanceOf<UserCreated>()`, then the smart-cast value: `result.user.email shouldBe email` |
| A rejection | the case, then the absence of any effect: `result.shouldBeInstanceOf<UserAlreadyExists>()` and `users.count() shouldBe 1` |
| A collection | `shouldHaveSize`, `shouldContainExactly` when the order is part of the behavior, `shouldContainExactlyInAnyOrder` otherwise |
| An exception | `shouldThrow<IllegalArgumentException> { money.convertWith(rate) }`. Check its message only when a caller reads it |
| An event | through the spy the fixtures hold: `spyEventPublisher.countEventsOfType(UserCreated::class) shouldBe 1` |
| Persisted state | read back through the port, `users.findEnabledBy(id) shouldBe user`, never through a double's internals |

Several properties of one value go in one `should { }` block, so a failure shows all of them at once.

## Time and identifiers

`ControlledClock` and `MockGenerateId` come with the fixtures. Fix the clock for a test that compares dates,
`controlledClock.fix(instant)`, and move it with `add(Duration.ofMinutes(16))` to cross an expiry.

## A port without its contract

Write its behavior once, in an abstract contract of the domain's test fixtures, then run it against each
implementation. Model: `ExchangeRatesTestContract`.

```kotlin
abstract class ExchangeRatesTestContract {

    private val repository: ExchangeRates by lazy { fetchRepository() }

    abstract fun fetchRepository(): ExchangeRates

    @Test
    fun `save an exchange rate and find it`() { … }
}

internal class InMemoryExchangeRatesTest : ExchangeRatesTestContract() {
    override fun fetchRepository(): ExchangeRates = InMemoryExchangeRates()
}

@JooqIntegrationTest(schemas = [DrinkitApplication::class])
internal class JooqExchangeRatesRepositoryTest : ExchangeRatesTestContract() {
    …
}
```

A test that passes for the in-memory double and fails for the adapter, or the reverse, is a difference between
them: report it, the contract is the behavior both must have.

## Pinning a surprising behavior

The test states what the code does today, and its name says so plainly, so a reader sees it is not a choice:

```kotlin
@Test
fun `accepts a cellar name made of spaces`() { … }
```

Then record the bug through the `new-issue` skill and give the issue's number in your report.

## Proving a test can fail

Break the behavior in the smallest way, run the one test, read the failure, then undo only that edit:

- invert a condition: `if (exists)` becomes `if (!exists)`;
- change a returned value or drop a side effect: return `null`, skip the `save`.

`git diff` shows your mutation alone before you undo it, and nothing after.
