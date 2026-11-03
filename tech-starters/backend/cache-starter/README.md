# Cache Starter

A [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/tech-starters.md)
that turns on Spring's cache abstraction, `@Cacheable` and `@CacheEvict`, as soon as a module depends on it, and gives
every cache a time to live and a maximum size. Each instance keeps its own entries in memory, with
[Caffeine](https://github.com/ben-manes/caffeine), so instances may serve different values for up to one time to live.

## What is it for?

Spring's cache abstraction separates what the code caches from where the entries are kept. A method annotated
`@Cacheable` names a cache, and a provider stores its entries: Caffeine in memory today, Redis tomorrow, with no
change to the code. Spring's default provider, a plain map, never expires an entry, so a value changed elsewhere
would be served forever. This starter brings Caffeine and sets the time to live of each cache in one place.
[ADR 0008](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/adr/0008-caching-through-spring-on-caffeine-with-a-ttl-per-cache.md)
says why.

## Configuration

| Property | Default | Environment variable | Required in production |
|---|---|---|---|
| `starters.cache.default-ttl` | `10m` | `STARTERS_CACHE_DEFAULTTTL` | No |
| `starters.cache.ttl.<cache>` | The `CacheTtl` of the module, see below, or `default-ttl` | `STARTERS_CACHE_TTL_<CACHE>` | No |
| `starters.cache.maximum-size` | `10000` | `STARTERS_CACHE_MAXIMUMSIZE` | No |
| `starters.cache.enabled` | `true` | `STARTERS_CACHE_ENABLED` | No |

`starters.cache.enabled=false` turns caching off: every `@Cacheable` method runs on each call.

## Usage

A module that caches declares the starter, which then wires itself:

```kotlin
dependencies {
    implementation(project(":cache-starter"))
}
```

A method of a Spring bean caches its result under a key, and the method that changes the data evicts it:

```kotlin
@Cacheable("exchange-rates", key = "#currency")
fun findRate(currency: Currency): ExchangeRate? = …

@CacheEvict("exchange-rates", key = "#rate.currency")
fun save(rate: ExchangeRate) { … }
```

A call from the same object skips the proxy that runs these annotations, which the
[architecture tests](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/writing-code/testing/architecture-tests.md)
forbid.

The module that owns a cache knows how long its data may be stale, so it declares that time to live as a bean, next
to its cache:

```kotlin
@Bean
fun exchangeRatesCacheTtl() = CacheTtl("exchange-rates", Duration.ofHours(1))
```

A cache takes, in this order, `starters.cache.ttl.<cache>` of the application, the `CacheTtl` of its module, and
`starters.cache.default-ttl`. A cache nobody declares is created on its first use with the default.

An eviction removes the entry from the instance that changed the data only, and the other instances serve the old
value until its time to live ends. Only cache what can be stale that long.

Inside a transaction, a put or an eviction waits for the commit, so a rollback leaves the cache as it was. Until
that commit, a read in the same transaction may still get the value cached before it.

A test moves the time of the caches forward with the `ControlledTicker` of the test fixtures, declared as a bean, in
place of waiting for an entry to expire:

```kotlin
testImplementation(testFixtures(project(":cache-starter")))
```

## Redis, later

When several instances must share their entries, or see an eviction at once, the provider will become Redis, with
the same names and times to live, as [#558](https://github.com/mboisnard/drinkit/issues/558) plans. A value cached
in Redis is serialized, so it must stay a type the serializer reads back, such as a string or a plain data class.
