---
title: "028. Draw identifiers as UUIDv7 in Crockford base32 from identifier-starter"
description: "Every identifier is a UUIDv7 written in lowercase Crockford base32, drawn by a plain function of identifier-starter rather than an injected port, so that one string serves the URL, the logs and the database and sorts by creation time."
navigation:
  title: "028 · Crockford UUIDv7 identifiers"
status: accepted
date: 2026-11-24
explainedIn:
  title: Identifier starter
  to: /en/reference/resources/tech-starters/identifier-starter
---

Every identifier of DrinkIt and of its starters is a UUIDv7 written in lowercase Crockford base32, 26 characters,
drawn by `create()` of its class rather than through an injected port. One string serves the URL, the logs
and the database, sorts by creation time, and stays a standard UUID once decoded.

## Context

Identifiers were MongoDB ObjectIds, 24 hexadecimal characters, which brought the `bson` library into an application on
PostgreSQL for one call. The domain drew them through a `GenerateId` port that built each identifier class by
reflection, so a class without a `(String)` constructor failed at runtime only, and no test relied on the values its
double drew. File identifiers were UUIDv4, in another format.

UUIDv7, RFC 9562 of 2024, puts the time in milliseconds in its first 48 bits, so identifiers drawn one after the other
sort in that order and insert at the end of a B-tree index. The Kotlin standard library draws it, `Uuid.generateV7()`
in Kotlin 2.4. Its canonical form takes 36 characters in a URL. TypeID writes the same 128 bits in 26 lowercase Crockford
base32 characters, an alphabet without `i`, `l`, `o` and `u`, and adds a type prefix in front. Nothing is deployed yet,
so the format changes without a migration.

## Decision

An identifier class extends `AbstractId` of `identifier-starter`, and its companion `IdFactory`, such as
`CellarId.create()` and `CellarId.parseOrNull(input)`. The class refuses a value of another format, and equals another
identifier of the same class and value only.

`create()` draws a UUIDv7 and writes it in lowercase Crockford base32, the suffix of a TypeID without its prefix. The
column holds that string, `VARCHAR(26)`, and the API, the logs and the URLs show the same one. `@JsonValue` on the
value makes Jackson write and read an identifier as its plain string, with no module to register.

The domain calls `create()` itself, with no port: an identifier carries no business rule, unlike the time that `Clock`
gives, so a test has nothing to control in its value. The architecture rule that keeps the system clock and `Random`
out of the domain also keeps out every other way to draw a UUID there.

A starter that owns a concept owns its identifier, extending `AbstractId`: `FileId` in `file-storage-starter`,
`VisitorId` in `security-starter`.
`identifier-starter` holds the mechanism and no identifier of its own. A starter API that receives an identifier
of the application, such as the id of a search document, keeps a `String`.

## Consequences

- One value for a file, a cellar or a user in the URL, the logs and a query, copied from one to the other as it is.
- A value can follow an object from one context to the next, the identifier of a bottle reused for its tasting
  sheet, since no prefix tells them apart.
- Identifiers sort by creation time, and their first 10 characters give it to a person reading the data. A business
  rule still reads the date it stored, since the identifier takes the system clock, not the injected one.
- An identifier reveals when its object was created, the sign-up date of a user through their `UserId`.
- The column is a string of 27 bytes rather than a native `uuid` of 16, and PostgreSQL's `uuid_extract_timestamp`
  does not read it.
- A test cannot predict a drawn identifier: it reads the one the code returned, or writes a fixed real one.
- `bson` is gone, and the encoder is ours to keep: two lines on `BigInteger`, readable rather than fast, where
  `typeid-java` would add a library.

## Alternatives

- **Keep ObjectId.** Rejected because it kept `bson` for one call and its date stops at the second. It offered the
  same string everywhere with no code.
- **UUIDv7 in a native `uuid` column, encoded at the edge.** Rejected because the identifier in a URL would differ
  from the one in a query, a converter away. It offered 16 bytes and `uuid_extract_timestamp`.
- **TypeID with a prefix, `cellar_01m86…`.** Rejected because the prefix forbids reusing a value across contexts. It
  offered the type in plain sight in a log, and can still be added at the edge of the API.
- **Keep the `GenerateId` port.** Rejected because it controlled values no test needed, at the cost of an injection
  in every use case and a double. It offered identifiers a test could predict.

## Sources

- [RFC 9562, Universally Unique IDentifiers](https://www.rfc-editor.org/rfc/rfc9562), IETF
- [TypeID specification](https://github.com/jetify-com/typeid), Jetify
- [Designing APIs for humans: object IDs](https://dev.to/stripe/designing-apis-for-humans-object-ids-3o5a), Stripe, for prefixed identifiers
