# 0007. Cache through Spring's abstraction, on Caffeine

- Status: accepted
- Date: 2026-11-03

A module that caches uses Spring's `@Cacheable` and `@CacheEvict`, and `cache-starter` stores the entries in memory
with Caffeine, with a time to live per cache. The code never names the cache library, so Redis can replace Caffeine,
once several instances need a shared cache, without a change to the code.

## Context

Some reads are frequent and change rarely, such as the settings an operator changes without a restart. Spring's
default cache is a map that never expires an entry, so a row changed in the database under a running instance was
never read again.

The maintainer wants the code to keep Spring's annotations, so that a demo environment runs in memory, and production,
once it runs several instances, shares its cache through Redis.

## Decision

`cache-starter` turns caching on and brings Caffeine. Each cache expires its entries after a time to live and holds a
bounded number of them, so no cache fills the memory. The module that owns a cache knows how stale its data may be,
and declares its time to live, which the application can override in its configuration. A put or an eviction made in
a transaction waits for its commit, so a rollback leaves the cache as it was. A module caches through the annotations
only, never through Caffeine's API. The starter's README gives the settings.

## Consequences

- A value changed elsewhere is read again within the time to live of its cache.
- Each instance keeps its own entries: an eviction reaches only the instance that changed the data, and the others
  serve the old value until its time to live ends. Redis removes that gap.
- Caching is on for the whole application as soon as a module depends on the starter.
- Nothing stops a module from calling Caffeine directly, and such a call would tie it to Caffeine.

## Alternatives

- **Spring's default map.** Rejected because its entries never expire, so a cached value never sees a change. It
  offered no dependency.
- **Redis now.** Rejected because it adds a server to every environment, the demo included, while one instance runs. It
  offered entries shared between instances.
- **A snapshot of each table reloaded on a schedule.** Rejected because every module that caches would need code of
  its own, with no path to a shared cache. It offered no library.

## Sources

- [Cache abstraction](https://docs.spring.io/spring-framework/reference/integration/cache.html), Spring
- [Caffeine](https://github.com/ben-manes/caffeine), Ben Manes
- [TwoHardThings](https://martinfowler.com/bliki/TwoHardThings.html), Martin Fowler, on why a cache is a cost
