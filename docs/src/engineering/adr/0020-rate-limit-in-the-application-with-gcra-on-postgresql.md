# 0020. Rate limit in the application with GCRA on PostgreSQL

- Status: accepted
- Date: 2026-11-09

The application limits how often a key calls a use case, with a starter of our own that runs the generic cell rate
algorithm, GCRA, and keeps one instant per key and limit in PostgreSQL. Only the code knows the use case, the user,
their role and the business key, and GCRA takes about thirty lines that no library offered on jOOQ.

## Context

Several features must stop a key that calls too often:

- **Sign-in codes**, which an attacker would request in bulk or guess, per email and per IP address.
- **LLM calls** of a user, counted in tokens rather than in calls, since each one costs money.
- **The catalog**, which an anonymous bot could dump page after page.
- **Importers**, which must not hammer the third-party sites they read.

A proxy or a WAF limits by IP and path. None of these needs fits: "3 codes per email", "an admin is not limited" or
"100,000 tokens per user and hour" are rules of the application. DrinkIt runs several instances, so a count must be
shared, and it runs PostgreSQL but no Redis.

Teams that limit at scale, Stripe, GitHub or Let's Encrypt among them, use the token bucket family, of which GCRA is
the form that stores one timestamp per key. Let's Encrypt moved to GCRA from fixed windows, whose counter resets at
the boundary: twice the limit passes across it, and a key that spent its limit early waits the whole window.

## Decision

- **GCRA.** A limit of `N` per period frees one call every `period / N`. Each limit of a key keeps its theoretical
  arrival, the instant its next call would be due at that pace. A call passes while that instant stays within one
  period of now, and moves it one interval later, times the cost of the call. The burst is the limit.
- **Computed in Kotlin, stored in PostgreSQL.** `drinkit_application.rate_limit` holds one row per rule, key hash
  and limit. A call locks its key with a transaction-scoped advisory lock, reads its rows, decides, and saves them
  in a transaction of its own, which then forgets a few rows whose arrival has passed.
- **Two entry points.** `@RateLimited` on a method names a policy, a bean that holds the rule and computes the key
  in Kotlin, or none to let the call through. `RateLimiter` serves code that limits by a cost or outside a request.
- **Several limits per rule**, all charged or none, such as 3 per 10 minutes and 10 per day.
- **Rules in code**, next to the use case they protect: a limit changes with a release.
- **A refusal answers 429** with `Retry-After`, and logs one warning without the key.

The starter offers no `RateLimit-*` headers, whose IETF draft is not final, and no memory store in production.

## Consequences

- A limit is written next to the use case it protects, and reads like the business rule.
- "5 per hour" is a rate, not a cap per clock hour: a key that spends its 5 calls at once gets one back every 12
  minutes, so up to 9 calls fit within one hour. A second, shorter limit tightens it.
- Each counted call costs one transaction of four statements, and a second connection when it runs inside another
  transaction. A traffic far above DrinkIt's would reopen the choice of a store.
- The instances compare their own clocks, which NTP keeps within milliseconds of each other.
- A database that fails makes the counted call fail: the sign-in stays closed rather than open.
- The rows read in SQL: a key's next due instant is in clear, its key a SHA-256 hash.
- The algorithm and its storage are ours to maintain.

## Alternatives

- **Bucket4j.** Rejected because its PostgreSQL store runs raw JDBC outside jOOQ and its transactions, keeps an
  opaque binary state, and reads two clocks, the database's and the instance's. It offered a mature token bucket and
  a Redis store for later.
- **bucket4j-spring-boot-starter or RateLimitJ.** Rejected because neither stores in PostgreSQL, the first has one
  maintainer and RateLimitJ is abandoned. They offered the annotation and the properties ready-made.
- **A fixed window per key.** Rejected because twice the limit passes across a boundary, and a key that spent its
  limit waits the whole window. It offered the simplest count, one integer per key.
- **A key and an exemption in SpEL on the annotation.** Rejected because the compiler checks a key written in
  Kotlin but not a string, and Laravel, ASP.NET Core and Rails take a policy in code too. It offered a rule read on
  the method.
- **A limit in the proxy or a WAF.** Deferred, as a defense in depth by IP that comes with the deployment. It offered
  no code, but knows neither the user nor the use case.

## Sources

- [Rate limiting, cells, and GCRA](https://brandur.org/rate-limiting), Brandur Leach
- [Scaling rate limits at Let's Encrypt](https://letsencrypt.org/2025/01/30/scaling-rate-limits/), Let's Encrypt
- [Scaling your API with rate limiters](https://stripe.com/blog/rate-limiters), Stripe
