# Event sourcing

An event-sourced aggregate is stored as its events, the list of what happened to it, and its state is the fold of
those events in order. DrinkIt does it with PostgreSQL tables and a small [`event-sourcing-starter`][starter], no
framework. Each new event updates the read model, the projection, in the same transaction, so a read always sees
the last write. Read this page before you store an aggregate as events or change one of its events. The user is the
first aggregate stored this way.

## 🧭 The pattern

An event states a fact in the past tense, such as `PromotedAsAdmin`, with its date and its author. We store an
aggregate this way for three reasons:

- **The history is the data.** Who changed what, and when, is kept for every change, not only the last state.
- **A projection is rebuilt, never migrated.** A new read model, or one that a bug left wrong, is computed again from
  the events.
- **Decisions see the whole history.** A use case can refuse a change because of what happened before.

It has a price. A stored event stays readable for as long as it exists: its name and its payload are a contract
with the past. Every read goes through a projection, and an aggregate takes more code than a table updated in
place. An aggregate whose history nobody needs stays a table.

## ⚙️ How DrinkIt applies it

Each event-sourced aggregate has an event table of its own, keyed by the aggregate's id and a sequence id. A use case
that changes the aggregate:

1. loads its history, the events in sequence order
2. folds it into the state a decision needs, which can differ from what reads need, and hands that state to a
   decider. A decider is a pure function of the [hexagonal architecture](hexagonal-architecture.md): it returns a
   refusal, or the event to append with the next sequence id.
3. saves the event through its port. The adapter appends it, folds the history again into the projection and saves
   it, in the same transaction.

The fold has a handler per event type, a function that applies the event to the state. A new event type without one is
ignored with no error, and the projection loses what it carries. The [`hexagonal-backend` skill][user-events] lists
every place a new event goes.

Two writes on the same aggregate can read the same history and compute the same next sequence id. The primary key
refuses the second one. The use case is annotated `@RetryableTransactional`, which runs it again on the new history,
up to three attempts. Its nested transaction rolls back the failed attempt only.

## ⚖️ The projection in the same transaction

Most event-sourced systems update their projections asynchronously, from a worker that catches up with the new
events. A read may then miss a write. DrinkIt updates them in the transaction of the event:

- **A read sees the last write,** and the frontend has no lag to handle.
- **Nothing runs beside the application.** No worker, no checkpoint, no catch-up after a crash.
- **The event and the projection commit together.** A failure leaves neither of them.

The write pays for it: each append folds the whole history. The projection also lives in the database of the events,
so a projection kept elsewhere, such as in a search index, cannot use this path. The same transaction fits while a
projection covers one aggregate and its history stays short. A projection that joins several aggregates, or one too
costly to compute on each write, would move to an asynchronous update.

## 🔢 Gaps between sequence ids

The sequence ids of an aggregate go 0, 10, 20, not 0, 1, 2. The gap lets an event be inserted later between two
others, at 15 for instance, and the events after it stay as they were. Inserting is how a missing event is caught
up when a bug forgot to write it, or how a migration splits one event into two. Most
event stores number without gaps: the gap is our choice, and the fold only needs the order.

## 🛠️ Rewriting the history

A business mistake is corrected by a new event, appended after the others, so that the history keeps both the
mistake and its correction. Rewriting stored events is a last resort, for two cases. A bug wrote a wrong event or forgot
one. Or the payload of an event changes shape, and we do not want to read the old shape forever. A one-off job does
it:

1. **Read both shapes,** when a payload changes shape. The code reads the old payload with a default for what it
   lacks, and a test reads an event stored in the old shape.
2. **Rewrite the events.** The job, open to administrators only, goes through the aggregates page by page. Each page
   has its own transaction, so a failure stops at one page and the job resumes from there. The job rewrites or
   inserts the events of each aggregate, skips one already done, then rebuilds its projection.
3. **Remove the job,** so that nobody runs it again, and the reading of the old shape, once every aggregate is
   migrated.

When SQL can express the rewrite, a [`dml/` changelog](../build-and-data/database.md) can do it instead of the job.
The projections still need the job's rebuild, since only the fold computes them. A rebuild is safe, since
the projection always comes from the whole history, never from a delta. No rebuild command exists yet: the first
rewrite adds one.

## 📚 Sources

- [Event Sourcing](https://martinfowler.com/eaaDev/EventSourcing.html), Martin Fowler
- [Event Sourcing pattern](https://learn.microsoft.com/en-us/azure/architecture/patterns/event-sourcing), Microsoft Azure Architecture Center
- [Retroactive Event](https://martinfowler.com/eaaDev/RetroactiveEvent.html), Martin Fowler
- [Guide to Projections and Read Models in Event-Driven Architecture](https://event-driven.io/en/projections_and_read_models_in_event_driven_architecture/), Oskar Dudycz
- [Rebuilding Event-Driven Read Models in a safe and resilient way](https://event-driven.io/en/rebuilding_event_driven_read_models/), Oskar Dudycz
- [Simple patterns for events schema versioning](https://event-driven.io/en/simple_events_versioning_patterns/), Oskar Dudycz
- [Versioning in an Event Sourced System](https://leanpub.com/esversioning), Greg Young
- [Inline projections](https://martendb.io/events/projections/inline.html), Marten

[user-events]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/hexagonal-backend/user-events.md
[starter]: https://github.com/mboisnard/drinkit/tree/master/tech-starters/backend/event-sourcing-starter
