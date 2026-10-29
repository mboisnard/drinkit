# The event-sourced User aggregate

`UserHistory.kt` and `UserHistoryDsl.kt` below are files, not classes: `git ls-files '*/<name>'` finds them.

## A use case that changes a user

Copy `DeleteUser`, whose decider refuses an author who may not edit the user:

1. Annotate it `@RetryableTransactional` from `event-sourcing-starter`, not `@Transactional`: two writes on the
   same user compete for the next sequence id, and the loser is retried.
2. Load the history with `userEvents.findAllBy(userId)`. No history means the user does not exist.
3. Rebuild the decision state with `UserDecision.from(history)` and hand it to the core, annotated
   `@Aggregate @FunctionalCore`.
4. The core returns the event to persist, numbered with `decision.nextSequenceId`, or a refusal.
5. The use case saves the event with `userEvents.save(event)`, which returns the `User` with the event applied.

Creating a user has no history to load: `CreateNewUser` builds the `Initialized` event without one.

## A new kind of event

The compiler finds some of the places, not all:

- the event, a `data class` implementing `UserEvent`, next to the others in `UserHistory.kt`;
- an `apply` in `User` and one in `UserDecision`, each registered in its `EventsReducer`. An event left out of a
  reducer is not an error: the default handler ignores what it carries, silently;
- its payload in `UserEventPayload` of the `-infra` module, both ways, and its `UserEventPayloadType`. The stored
  event name is that enum constant's name: renaming one makes the events already stored unreadable;
- a builder in `UserHistoryDsl.kt`, in the test fixtures of the `-domain` module, so tests can start from a
  history that holds it.
