# Event Sourcing Starter

A [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/tech-starters.md)
with the building blocks of an event-sourced aggregate in DrinkIt. The
[event sourcing page](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/event-sourcing.md)
explains the pattern and how DrinkIt applies it. The starter declares no Spring bean, so it has no
auto-configuration.

## Key Features

- **`History.from`** refuses events whose sequence ids do not strictly increase.
- **`SequenceId.next()`** adds 10, for the
  [gaps between sequence ids](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/event-sourcing.md#gaps-between-sequence-ids).
- **`EventsReducer`** sends an event type with no handler to the default handler, which leaves the state as it is
  unless you pass your own.
- **`@RetryableTransactional`**, on a use case, runs it again when its event store adapter throws
  `DuplicateSequenceException`, three attempts at most. Each attempt runs in a new transaction, or in a savepoint
  when a transaction is already open, so a refused attempt leaves that transaction usable.

## Usage

A module that stores an aggregate as events declares the starter in its `build.gradle.kts`, as
[ADR 0011](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/adr/0011-a-module-declares-the-event-sourcing-starter-itself.md)
records. The starter brings
`retryable-starter`, which makes the retry of `@RetryableTransactional` run:

```kotlin
dependencies {
    implementation(project(":event-sourcing-starter"))
}
```

The events implement `DomainEvent`, the state has a function per event, and a reducer folds a history, as in
[`CarExample.kt`](https://github.com/mboisnard/drinkit/blob/master/tech-starters/backend/event-sourcing-starter/src/test/kotlin/com/drinkit/event/sourcing/CarExample.kt):

```kotlin
sealed interface CarEvent : DomainEvent
data class CarCreated(override val sequenceId: SequenceId, val name: String) : CarEvent
data class CarPurchased(override val sequenceId: SequenceId, val owner: String) : CarEvent

data class Car(val name: String, val owner: String? = null) {
    fun apply(event: CarPurchased) = copy(owner = event.owner)
}

val reducer = EventsReducer<Car, CarEvent, CarCreated>(factory = { Car(it.name) })
    .register<CarPurchased>(Car::apply)

val events = listOf(CarCreated(SequenceId(), "DB5"), CarPurchased(SequenceId(10), "James Bond"))
val car = reducer.reduce(History.from<CarEvent, CarCreated>(events))
```
