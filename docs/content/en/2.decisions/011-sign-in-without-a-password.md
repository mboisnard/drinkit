---
title: "011. Sign in without a password"
description: "Users sign in with a passkey, Google or Apple, or a six-digit code sent by email, on one screen that also signs them up."
navigation:
  title: "011 · Sign in without a password"
status: accepted
date: 2026-11-06
explainedIn:
  title: Security principles
  to: /en/reference/security/principles
---

A user signs in with a passkey, with Google or Apple, or with a one-time code sent to their email, and DrinkIt stores
no password. A password reset already makes an account only as safe as its mailbox: a password adds a way in without
raising that ceiling, and it asks users for the most effort.

## Context

DrinkIt serves very different people. Most are everyday drinkers, on their phone at home or in a supermarket aisle,
who scan a bottle of wine, beer or spirits to keep it, rate it or count what they have. They give up when signing up
asks for a password and an email confirmation first, or when the app signs them out often. Others are professionals,
such as a wine shop or a bar, who manage long lists of bottles, sometimes as a team, on a computer as much as on a
phone. They expect a sign-in that their whole team can use every day, and that keeps their data safe.

A password brings its own threats and its own work: users reuse passwords, so a leak elsewhere opens DrinkIt, a leak
of DrinkIt's hashes exposes the weak ones, and it needs a policy, a breached-password check, a reset by email and a
limit on failed tries. A passkey is the only common method that resists phishing, since the browser signs a challenge
only for the site the passkey was created on, and it is used when the sign-in offers it by default.

## Decision

One screen serves both sign-up and sign-in, with no choice to make between them:

```mermaid
flowchart TB
  screen["Sign-in screen<br/>email field, passkey, Google and Apple buttons"]
  screen -- "the passkey button" --> passkey[Passkey]
  screen --> provider[Google or Apple]
  screen -- "an email typed" --> code["Six-digit code<br/>sent by email"]
  passkey --> session([Session])
  provider -- "an email the provider verified" --> session
  code -- "a known email: its account<br/>an unknown one: a new account" --> session
  session -- "no passkey yet" --> offer["Offer to create one, once"]
```

- **Passkey first.** A returning user signs in with a fingerprint or a face, and a browser without passkeys shows the
  rest of the screen unchanged. The passkeys are Spring Security's, at its URLs and in its tables, on the W3C
  WebAuthn specification: the authenticator must verify the user. A "Sign in with a passkey" button opens the
  browser's dialog of the passkeys, rather than their autofill in the email field, where a user sees neither when nor
  how the sign-in starts.
- **Google and Apple.** An account is found by the provider's id, then by an email the provider marks as verified.
- **A code sent by email.** Six digits, single use, valid for minutes, refused after a few wrong tries and stored as
  a hash. The same answer comes back for any email, so the screen reveals no account.
- **The mailbox is the root of trust.** A code recovers an account whose passkey or provider is lost, and a sensitive
  change, such as a new email, asks for a passkey or a fresh code.

DrinkIt offers no SMS code, which SIM swapping and costs rule out, and no authenticator app code, which a phishing
page relays like any code. The password sign-in built first goes away. The session that follows any method is the one
of [ADR 012](./012-keep-sessions-in-postgresql.md).

## Consequences

- No secret of a user is stored, so a leak of the database exposes no credential.
- Sign-up, sign-in and recovery are one flow, with no password policy, reset or hashing to maintain.
- A sign-in without a passkey, Google or Apple waits for an email: a deployment needs an email provider that delivers
  within seconds, and a mail outage stops those sign-ins.
- A code can be phished in real time like a password. Only a passkey resists phishing, which is why it comes first.
- A security key without a PIN creates no passkey, since the authenticator must verify the user.
- The session stays in JSON, as [ADR 012](./012-keep-sessions-in-postgresql.md) chose, so the starter reads back the
  options of a passkey that Spring Security keeps in it.
- The code endpoint is public and sends emails, so it needs limits per email and per address, and a captcha.
- Apple needs a public domain over HTTPS and a developer account.
- A professional used to a password kept in a password manager loses that habit. Their demand would reopen the
  decision.

## Alternatives

- **Email and password.** Rejected because it adds the threats above, and its reset already hands the account to the
  mailbox. It offered a familiar sign-in, fast with a password manager.
- **A password and a code at each sign-in.** Rejected because it asks the most effort at every sign-in, and a phishing
  page relays both. It offered two factors.
- **Passkeys alone.** Rejected because an old browser or a lost device still needs the email, so the code exists
  anyway. It offered phishing resistance at every sign-in.
- **A passkey whose user verification is only preferred**, the default of the specification. Rejected because a
  passkey that skips it is only something the user has, not a strong factor of its own. It offered security keys
  without a PIN.
- **A link sent by email instead of a code.** Rejected because the mail app opens it in another browser than the one
  that asked. It offered one click.

## Sources

- [Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html), OWASP
- [Passkeys](https://fidoalliance.org/passkeys/), FIDO Alliance
- [Web Authentication Level 3](https://www.w3.org/TR/webauthn-3/), W3C
- [Passkeys](https://docs.spring.io/spring-security/reference/servlet/authentication/passkeys.html), Spring Security
- [Passkeys: A Shattered Dream](https://fy.blackhats.net.au/blog/2024-04-26-passkeys-a-shattered-dream/), William
  Brown, on what passkeys still get wrong
