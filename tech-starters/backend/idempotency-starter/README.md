# Idempotency Starter

A [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/architecture/tech-starters.md)
that runs an action once per key, however many times it is sent:

- **A client retries a request**, after a double click or a timeout: one cellar, not two.
- **A broker delivers an event again**, since delivery is at least once: one welcome email, not two.
- **A request is retried after its commit**, following a server error: the events it publishes again carry the same
  keys, so their handlers skip them, and so do the providers it calls.

It keeps each key in PostgreSQL for 24 hours, with what the first run answered. It is built in-house, as
[ADR 0019](https://github.com/mboisnard/drinkit/blob/master/docs/src/engineering/adr/0019-run-requests-and-events-once-per-idempotency-key.md)
explains.

```mermaid
flowchart LR
    Client -- "Idempotency-Key" --> Endpoint["@Idempotent endpoint"]
    Endpoint -- "publishes, derived key" --> Handler["idempotent handler"]
    Handler -- "Idempotency-Key, derived" --> Provider[(Provider API)]
    Endpoint & Handler --> Keys[("PostgreSQL<br/>drinkit_application.idempotency_key")]
```

## How it works

A key is taken before the action runs, in a transaction of its own, so that every instance sees it. Its holder
keeps it for a lease of 5 minutes. The action then ends in one of three ways:

```mermaid
stateDiagram-v2
    [*] --> Running: key taken
    Running --> Done: action succeeded, answer recorded
    Running --> [*]: action failed or rolled back, key freed for the next try
    Running --> Running: lease ended, another run takes over
    Done --> [*]: retention ended, key forgotten
```

The answer is recorded in the transaction open around the action, when there is one. An `@Idempotent` endpoint
always runs in one, opened by the starter: the cellar and the answer commit together, so a crash before the commit
leaves neither, and the retry runs again once the lease ends. A run that outlives its lease cannot record its answer
once another run took the key over: it fails, and its writes roll back.

A request sent again with the same key gets one of four answers:

```mermaid
sequenceDiagram
    participant Client
    participant Starter
    participant Endpoint
    Client->>Starter: POST /api/cellars, Idempotency-Key K
    Starter->>Endpoint: first time: runs it
    Endpoint-->>Client: 201, Location, body
    Client->>Starter: same request, same K
    Starter-->>Client: replay: 201, Location, body, without running it
    Client->>Starter: same K, while the first one runs
    Starter-->>Client: 409, Retry-After: 1
    Client->>Starter: same K, another body
    Starter-->>Client: 422
```

The key of an event follows the key of what published it. A retry of a request, after a server error that came
once its transaction had committed, publishes the same events under the same keys, so its handlers skip them:

```mermaid
flowchart LR
    K["request key K<br/>of user U on createCellar"] --> E1["1st CellarCreated<br/>sha256(K, type, 1)"]
    K --> E2["2nd CellarCreated<br/>sha256(K, type, 2)"]
    E1 --> H["handler mail.delivery.queue<br/>its own key, from the event's"]
    H --> P["provider call<br/>Idempotency-Key derived"]
    N["no current key"] --> E3["event<br/>a random key"]
```

## Usage

```kotlin
dependencies {
    implementation(project(":idempotency-starter"))
}
```

The application declares a `Clock` bean, which dates the keys.

**An endpoint** gets `@Idempotent` on the method of its controller, the override of the interface generated from
the API contract included, and declares the optional `Idempotency-Key` header in the contract:

```kotlin
@Idempotent
override fun createCellar(createCellarRequest: CreateCellarRequest, idempotencyKey: String?): ResponseEntity<CellarId>
```

- A request without the header runs as before. The client draws one key per action, a UUID, and sends it again on
  each retry of that action.
- The key belongs to the user, or else to the visitor, and to the endpoint: two users never share one.
- A replay gets the status, the `Content-Type`, the `Location` and the body of the first answer, never its cookies.
- The endpoint, its use case included, runs in one transaction with the key. A `5xx`, a `429`, an exception or a
  refusal whose transaction is marked for rollback roll its writes back and free the key: the next try runs again.
- `409` while the first request runs, `422` for a key sent with another method, path or body.
- `400` for a blank key or one over 255 characters, `413` for a body over 64 KB: the fingerprint, of the method,
  path, query string and body, reads at most that much.
- An endpoint answers a body under 64 KB: a larger answer fails, and rolls its writes back.

**A handler** of the messaging starter is idempotent with one attribute:

```kotlin
@PlatformEventHandler("mail.delivery.queue", idempotent = true)
fun deliver(command: SendEmail)

@PlatformEventHandler("order.paid.queue", idempotent = true, idempotencyKey = "#event.orderId")
fun ship(event: OrderPaid)
```

- The key is the delivery's, or the one `idempotencyKey` reads from the event, a SpEL expression on `#event`.
- A delivery that finds its key running on another instance waits for it, without spending an attempt.

**Any other code** calls `Idempotency` itself, with a key that names one action and is the same on each of its tries:
drawn once by whoever starts the action, or read from the business data, never from a clock or a random value drawn
again on each try.

```kotlin
when (val result = idempotency.once(scope = "import.wines", key = fileId) { importWines(file) }) {
    is IdempotentResult.Executed -> result.value
    is IdempotentResult.AlreadyDone -> null
    is IdempotentResult.InFlight -> throw TryLater(result.retryAfter)
    IdempotentResult.Mismatch -> error("Never: no fingerprint given")
}
```

Keying an email of a sign-in code by the event that carries the code sends it once per event, and a second request for
a code, which issues a new code, sends a second email: a key built from the email address alone would drop it.

`CurrentIdempotencyKey` holds the key of the request or the event being handled, and `idempotency.key` of the MDC
copies it into the logs.

**A provider** whose API honours `Idempotency-Key` is listed in `starters.webclient.retry.idempotent-groups` of the
web client starter: its `POST`, `PUT`, `PATCH` and `DELETE` then carry a key derived from the current one, and are
retried on a network error.

## Configuration

| Property | Default | Environment variable |
|---|---|---|
| `starters.idempotency.enabled` | `true`, `false` registers no `Idempotency` | `STARTERS_IDEMPOTENCY_ENABLED` |
| `starters.idempotency.retention` | `24h`, how long a key is kept | `STARTERS_IDEMPOTENCY_RETENTION` |
| `starters.idempotency.lease` | `5m`, after which a request or a handler whose instance crashed runs again on the next try | `STARTERS_IDEMPOTENCY_LEASE` |

## Tests

`inMemoryIdempotency()`, from `testFixtures(project(":idempotency-starter"))`, keeps the keys in memory. A test of
an idempotent handler without PostgreSQL declares it as its `Idempotency` bean.

## Limits

- A handler that outlives its lease, such as an email held by a slow mail server, may run twice: an email cannot
  roll back.
- A handler outside a transaction that succeeds while the database fails to keep its outcome frees its key: its
  retry runs again.
- The answer of a request stays in clear in the table for 24 hours.
- The current key lives on the thread: code that hands work to another thread passes the key along itself.
