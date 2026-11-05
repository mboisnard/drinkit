# 0014. Argon2id for passwords, BouncyCastle as a dependency

- Status: accepted
- Date: 2026-11-05
- Issue: [#590](https://github.com/mboisnard/drinkit/issues/590)

Passwords are hashed with Argon2id only, which takes BouncyCastle as a dependency, and every hash carries the id of
its algorithm. bcrypt ignored what a password holds past 72 bytes, and its hashes had no id to change the algorithm by.

## Context

Passwords were hashed with bcrypt, without the `{bcrypt}` prefix that Spring Security's `DelegatingPasswordEncoder`
reads to pick an algorithm, so the algorithm could never change. bcrypt also reads only the first 72 bytes of a
password. Sixty-four accented characters take 128 bytes in UTF-8: the second half would go unchecked, while ASVS 5.0
asks for passwords of at least 64 characters to be accepted (requirement 6.2.9).

DrinkIt is not deployed yet: the only stored hash is the one of the demo user, which a changelog writes. No hash of a
real user has to be read in bcrypt.

OWASP's Password Storage Cheat Sheet lists Argon2id first. Spring Security's `Argon2PasswordEncoder` computes it
through BouncyCastle, which DrinkIt did not depend on.

## Decision

The starter hashes and checks passwords with a `DelegatingPasswordEncoder` whose only algorithm is Argon2id:

- it writes `{argon2}` hashes with the minimum of the OWASP cheat sheet: 19 MiB of memory, 2 iterations, 1 lane,
  rather than Spring Security's defaults, which use 16 MiB
- it reads no other hash: a bcrypt hash, or one without a prefix, fails the login
- the `{argon2}` prefix stays on each hash, so that a later change of algorithm can still read the hashes stored
  before it
- the demo data holds an `{argon2}` hash of the demo password
- `bcprov`, BouncyCastle's provider, joins the version catalog

No hash is migrated at login. A stronger parameter set later applies to new and changed passwords, and the day a
stored hash must change, a migration is added for it.

## How it is checked

`LoginTest` of `security-starter`: a password of 64 accented characters is accepted and verified with Argon2id.
`SecurityStarterAutoConfigurationsTest` checks that the encoder writes `{argon2}` hashes.

## Consequences

- A password is checked in full, whatever its length.
- The algorithm or its parameters can change again: the prefix says how each stored hash was computed.
- Nothing upgrades an existing hash: new parameters reach a hash only when its owner sets a new password, or through
  a migration written for that change.
- A local database filled before this change keeps the bcrypt hash of the demo user, which no longer logs in until
  the database is recreated.
- Each login holds 19 MiB of memory while it hashes. On virtual threads, no more hashes run at once than there are
  processors, since a hash never gives its thread back. The starter caps them at one per processor all the same, so
  that an application on platform threads slows a burst of logins down rather than exhausting its memory.
- The column of the password hash widens to 255 characters: an `{argon2}` hash takes about 105.
- BouncyCastle is one more cryptographic library to keep up to date.

## Alternatives

- bcrypt kept, with a prefix: no dependency, but passwords still cut at 72 bytes.
- bcrypt over a SHA-256 of the password: no truncation, but a construction of our own, which the cheat sheet warns
  against.
- PBKDF2: in the JDK, with no dependency, but OWASP keeps it for FIPS-140 compliance, which DrinkIt does not need.
- scrypt: second on OWASP's list, and Spring computes it through BouncyCastle too.

## Sources

- [Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html), OWASP
- [Application Security Verification Standard 5.0](https://github.com/OWASP/ASVS/tree/v5.0.0), OWASP
- [Password Storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html), Spring Security
