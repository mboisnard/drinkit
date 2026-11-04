# Configuration Starter

A [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/tech-starters.md)
that keeps in PostgreSQL the settings an operator changes without a restart, such as a limit or a switch. A class
injects `Configurations` and reads `configurations[key]`, a scalar or a data class stored as JSON, whose key
`configurationKey(name, default)` holds the default in the code. Every instance sees a change within 10 minutes. No
admin page writes them yet: a change is an SQL update of the table, or a write in a transaction.

## What is it for?

A setting fixed when the application starts, such as a URL, a timeout or a credential, is a
`@ConfigurationProperties` class. A feature released to some users first, or one user at a time, goes through the
[feature flags starter](https://github.com/mboisnard/drinkit/blob/master/tech-starters/backend/feature-flags-starter/README.md).
This starter is for the rest: a value the same for every user, which an operator changes while the application
runs, and which the instances read again by themselves.

A secret never goes here: whoever reads the database reads the table in clear.

## Configuration

| Property | Default | Environment variable | Required in production |
|---|---|---|---|
| `starters.cache.ttl.configurations` | `10m` | `STARTERS_CACHE_TTL_CONFIGURATIONS` | No |
| `starters.configuration.enabled` | `true` | `STARTERS_CONFIGURATION_ENABLED` | No |

The reads go through the `configurations` cache of the
[cache starter](https://github.com/mboisnard/drinkit/blob/master/tech-starters/backend/cache-starter/README.md), which
sets that delay. `starters.configuration.enabled=false` registers no `Configurations`, for an application that reads
none. The changelogs of the
[updater](https://github.com/mboisnard/drinkit/blob/master/deployment/updater/README.md) create the table,
`drinkit_application.configuration`, never the application.

## Usage

The module that reads a configuration declares the starter, which then wires itself and gives the module's
classes a `Configurations` bean to inject. The application must declare a `Clock` bean, which dates each write:

```kotlin
dependencies {
    implementation(project(":configuration-starter"))
    testImplementation(testFixtures(project(":configuration-starter")))
}
```

### Declare a key

A key holds, in one data class, the settings that one adapter or starter reads. The data class is stored as one
JSON value in one row, so that its fields are checked together and change together. The defaults of its fields are the defaults of the
settings, and its `init` block holds the rule a value must follow:

```kotlin
data class RetryPolicy(val maxAttempts: Int = 3, val delay: Duration = Duration.ofSeconds(1)) {
    init {
        require(maxAttempts in 1..10) { "maxAttempts must be between 1 and 10" }
    }
}

val retryPolicy = configurationKey("partner-api.retry-policy", default = RetryPolicy())
val maintenanceMode = configurationKey("maintenance-mode", default = false)
```

The name of the key identifies its row: a renamed data class keeps its stored value, a renamed key loses it.

### Read it

```kotlin
internal class PartnerApiClient(private val configurations: Configurations) {

    fun send(order: Order) {
        val policy = configurations[retryPolicy] // the stored value, or RetryPolicy() when none is stored
        // …
    }
}
```

A read never writes its default to the table, so a default changed in the code applies to every key with no
stored value. A stored value
missing a field takes the field's default, and a field the class no longer has is ignored. A stored value that
cannot be read, or that breaks the rule of its class, is logged at `ERROR` with its key, and the default applies.
A field renamed in the code would lose the value an operator set: a changelog renames it in the stored values, as
the [database page](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/build-and-data/database.md)
says.

### Change it

From the code, `set` and `reset` write in the transaction of the caller, so that a value changes with the other
writes of the same use case, or not at all. Outside a transaction, they fail:

```kotlin
@Transactional
fun changeRetryPolicy(maxAttempts: Int) {
    configurations[retryPolicy] = RetryPolicy(maxAttempts = maxAttempts)
}

@Transactional
fun restoreTheDefaultRetryPolicy() {
    configurations.reset(retryPolicy)
}
```

Or by SQL, while no admin page exists:

```sql
INSERT INTO drinkit_application.configuration (key, value, modified)
VALUES ('partner-api.retry-policy', '{"maxAttempts": 5}', now())
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value, modified = EXCLUDED.modified;
```

Once the transaction commits, a write from the code evicts the key from the cache of its instance, and a rollback
leaves that cache as it was. That instance then reads the new value, unless a read that started before the commit
puts the old one back in its cache. It then reads the new value once that entry expires, as the other instances do,
and every instance after an SQL update.

### Read it from the domain

The domain does not depend on this starter, as the
[hexagonal architecture](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/hexagonal-architecture.md)
wants. It declares the data class and a port, and an adapter of `drinkit-infra`, the module of the adapters, implements the port:

```kotlin
// drinkit-domain
fun interface RetryPolicies {
    fun current(): RetryPolicy
}

// drinkit-infra
internal class ConfiguredRetryPolicies(private val configurations: Configurations) : RetryPolicies {
    override fun current() = configurations[retryPolicy]
}
```

### Test it

The test of a consumer gives it the `InMemoryConfigurations` of the test fixtures:

```kotlin
val configurations = InMemoryConfigurations().apply { this[retryPolicy] = RetryPolicy(maxAttempts = 1) }
```
