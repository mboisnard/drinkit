# 0011. A module declares the event sourcing starter itself

- Status: accepted
- Date: 2026-11-04
- Issue: [#394](https://github.com/mboisnard/drinkit/issues/394)

A module that event-sources an aggregate adds `event-sourcing-starter` to its own dependencies. The
`library-convention` no longer gives it to every library, because only the domain and its adapters use it.

## Context

The `library-convention` archetype gave `event-sourcing-starter` to each of the 14 modules that apply it. Measured on
2026-11-04, only `drinkit-domain` and `drinkit-infra` use its types. The 12 tech starters among those 14 received
`History`, `EventsReducer` and `SequenceId` on their compile classpath. They also received `retryable-starter` and
its auto-configuration at runtime, since the event sourcing starter brings it for `@RetryableTransactional`.

The same archetype also gives `kotlin-starter`. That starter is a shared toolbox that every module may use, and it
stays there.

## Decision

`library-convention` adds Spring's context and transactions and `kotlin-starter`, nothing more.

`drinkit-domain`, which holds the aggregates, and `drinkit-infra`, which stores their events, declare
`implementation(project(":event-sourcing-starter"))`. The domain's test fixtures declare it too. A future module that
stores events does the same. The application still receives the starter and `retryable-starter` through the domain.

## How it is checked

A module that uses an event sourcing type without declaring the starter does not compile.

## Consequences

- A tech starter's classpath shows only what it depends on, and `retryable-starter` no longer reaches the tests of a
  starter that never retries.
- A new event-sourced module writes one more dependency line.

## Alternatives

- Keep the starter in `library-convention`: no line to write, but 12 modules carry a dependency they never use, and
  the retry auto-configuration with it.
