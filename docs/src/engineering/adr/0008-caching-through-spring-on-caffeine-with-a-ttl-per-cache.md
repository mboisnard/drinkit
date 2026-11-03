# 0008. Caching through Spring's abstraction, on Caffeine, with a time to live per cache

- Status: accepted
- Date: 2026-11-03
- Issue: [#542](https://github.com/mboisnard/drinkit/issues/542)

Every module that caches, an application module or a starter, uses Spring's `@Cacheable` and `@CacheEvict`, and the
[tech starter](../architecture/tech-starters.md) `cache-starter` stores the entries with Caffeine, with a time to live
per cache, so that a value changed elsewhere is read again. Each instance keeps its own entries until Redis replaces
Caffeine, without a change to the code.

## Context

`configuration-starter` keeps in a table the settings an operator changes without a restart, and cached its reads
with `@Cacheable` on Spring's default provider, a map that never expires an entry: a row changed in the database
underneath an instance was never read again (#542). The default map has no time to live. The maintainer wants the
code to keep Spring's annotations, so that a demo environment runs in memory and production, once it runs several
instances, shares its cache through Redis ([#558](https://github.com/mboisnard/drinkit/issues/558)).

## Decision

`cache-starter` turns on `@EnableCaching`, so that a module only declares the dependency, and brings Caffeine, whose
version comes from Spring Boot's BOM. Each Caffeine cache expires its entries after a time to live and holds at most
`starters.cache.maximum-size` entries, so that no cache fills the heap. The module that owns a cache knows how stale
its data may be, and declares its time to live as a `CacheTtl` bean, a cache name and a duration. The application
overrides it with `starters.cache.ttl.<cache>`, and a cache no one declares takes `starters.cache.default-ttl`. A
module caches through `@Cacheable` and `@CacheEvict`, never through Caffeine's API. A put or an eviction made in a transaction waits
for its commit, through Spring's `TransactionAwareCacheManagerProxy`, so a rollback leaves the cache as it was.

## How it is checked

`CacheStarterAutoConfigurationTest` asserts the time to live a cache gets from each source, and its maximum size.
Nothing stops a module from calling Caffeine directly.

## Consequences

- A value changed elsewhere is read again within the time to live of its cache.
- Each instance keeps its own entries: an eviction reaches only the instance that changed the data, and the others
  serve the old value until its time to live ends. Redis removes that gap.
- Caching is on for the whole application as soon as a module depends on the starter.
- One more library on the classpath, whose version moves with Spring Boot's.

## Alternatives

- Spring's default map: no dependency, and no time to live.
- A snapshot of each table, reloaded on a schedule: no dependency, but code of its own for every module that
  caches, and no path to a shared cache.
- Redis now: entries shared between instances, but a server to run in every environment, the demo included.
