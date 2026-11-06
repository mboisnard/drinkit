# 0011. Keep sessions in PostgreSQL

- Status: accepted
- Date: 2026-11-06

A signed-in browser holds a random session id in a cookie, and the session itself is a row in PostgreSQL, rather than
in Redis or in a token. The application runs with no other server to start, and a session ends the moment its row is
deleted. Its attributes are stored as JSON, so that the classes behind them can change without signing every user out.

## Context

The browser that runs the Nuxt app is the only client of the API, on the same origin. Signing a user out everywhere,
after a role change, a deletion or a theft, must take effect at once. DrinkIt already runs PostgreSQL in every
environment, the local start included, and runs no Redis.

Spring Session stores a session's attributes with Java serialization by default: bytes that nobody can read, that a
changed class no longer reads, and whose reading is a known path for attacks.

## Decision

The [security starter](../security/principles.md) keeps one row per session through Spring Session JDBC, and the
cookie carries only its id, 256 random bits.

- **PostgreSQL rather than Redis.** The sessions live in the database the application already has. The choice can be
  challenged again if the sessions turn out to load the database too much or to slow requests down.
- **Attributes as JSON.** The attributes are written as JSONB, from an allowlist of types. A property added or
  removed, or a class renamed, is handled in the conversion instead of breaking every stored session, and a row that
  can no longer be read signs only its user out.
- **Sessions that end.** A session also ends on its own:

| Duration | Value | Why |
|---|---|---|
| Idle timeout | 30 minutes | A device left unattended stops being signed in |
| Absolute lifetime | 8 hours since the sign-in | A stolen session id stops working the same day, even when kept busy |

A session that ends can start again from the remember-me cookie of
[ADR 0012](0012-remember-me-with-a-rotating-hashed-token.md). A user may be signed in on any number of devices, and
signing out everywhere deletes all their rows at once.

## Consequences

- No other server to run: the application starts with PostgreSQL alone, in every environment.
- A revocation takes effect at the next request, on every instance.
- Each request reads its session from PostgreSQL, and writes it back when it changes: a load on the database that
  Redis would have taken.
- A row reads as plain JSON in `psql`.
- The starter owns the conversion to JSON and the queries that read and write it, which a Spring Session upgrade can
  break.

## Alternatives

- **Redis.** Rejected because it adds a server to every environment for one table's worth of data. It offered speed
  and expiry built in.
- **A signed token such as a JWT.** Rejected because a revocation waits for the token's expiry or needs a deny list,
  which is state again, and a token in the browser's storage is open to any injected script. It offered no lookup per
  request.
- **Spring Session's Java serialization.** Rejected because its rows cannot be read, and a changed class breaks every
  stored session. It offered no code to write.

## Sources

- [Session Management Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html), OWASP
- [Spring Session JDBC](https://docs.spring.io/spring-session/reference/configuration/jdbc.html), Spring
- [JSON Web Token Best Current Practices, RFC 8725](https://www.rfc-editor.org/rfc/rfc8725), IETF, the token
  alternative done right
