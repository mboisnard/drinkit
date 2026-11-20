---
title: "025. Follow the backend's session in the Nuxt app, and declare the access of every page"
description: "The Nuxt app holds no session of its own: security-starter reads the current user from the backend and speaks its sign-in protocol, and every page declares who may open it, closed when it forgets."
navigation:
  title: "025 · Session of the Nuxt app"
status: accepted
date: 2026-11-20
explainedIn:
  title: Calling the API
  to: /en/reference/security/calling-the-api
---

The Nuxt app keeps no session of its own: `security-starter` reads the current user from the backend, which holds
the session, and calls the backend's sign-in protocol itself. Every page declares who may open it, and a page that
forgets fails the type check, so that a page is never open by omission.

## Context

The backend holds the session in an HttpOnly cookie and decides every access
([ADR 012](./012-keep-sessions-in-postgresql.md), [ADR 011](./011-sign-in-without-a-password.md)). The app still has to
know who is signed in, on the server for the first render and in the browser after, to show a page or send the user
to sign in.

The auth modules of the Nuxt ecosystem keep a session of their own, a sealed cookie or a token, next to the
backend's. The app's client is generated from each app's contract ([ADR 024](./024-generate-the-frontend-api-client-with-hey-api-at-each-build.md)),
and a starter never imports it. A page that should be protected and declares nothing shows its frame to anyone,
while the backend refuses its data.

## Decision

**The backend's session is the only one.** `security-starter` reads the current user from a path the app names, once
per app: on the server for the first page, whose payload hands it to the browser, then after a sign-in, a sign-out
or a change of roles. A 401 to a signed-in user forgets them and sends them to sign in, back to the page after.

**The starter speaks the sign-in protocol of the backend `security-starter`.** It calls `/api/auth/*` under the API's
path with the options of `useApi()`, outside the generated client. The app names only its current user path, and
types the user by registering its contract's type. Each way of signing in, an email code today, ends with one shared
step: the user read, the `drinkit:signed-in` hook, the return URL kept when it stays on the app's origin.

**Every page declares its access.** `definePageMeta({ auth })` is `'public'`, `'signed-in'` or a role, written as a
literal. The build lists the pages without it into a file that fails the type check, and a page without it is
treated as `'signed-in'` at run time. A visitor goes to sign in. A signed-in user without the role gets a 403, or the
page the app gives one of their roles, such as the registration for an account not completed.

The access of a page decides what the app shows. The backend still checks every call.

## Consequences

- One line per page says who opens it, and a page forgotten is closed, not open.
- `useCurrentUser()` gives the user, their roles and the sign-out anywhere, on the server and in the browser, with no
  call of its own.
- A passkey or Google and Apple add a composable that ends with the shared step: the state, the middleware and the
  pages do not change.
- The starter carries the paths of the backend's sign-in protocol and the type of its requests by hand: a change of
  that protocol changes the starter and its double, which the contract suite run on the real backend catches.
- An `auth` held in a constant rather than written as a literal is not read by the build, and the page fails the
  type check.

## Alternatives

- **An auth module of the ecosystem, nuxt-auth-utils or sidebase's.** Rejected because each keeps a session of its
  own beside Spring's, two sessions to expire together. They offered composables and a middleware ready to use.
- **The app hands the starter its generated functions.** Rejected because each app would write the same plugin to
  wire the sign-in. It offered requests typed by the contract.
- **A middleware a page opts into, as a `middleware: ['auth']` meta.** Rejected because a page that forgets it is
  open. It offered no rule on the pages that need none.

## Sources

- [nuxt-auth-utils](https://github.com/atinux/nuxt-auth-utils), Nuxt, the session kept by the app
- [Protecting pages](https://auth.sidebase.io/guide/application-side/protecting-pages), sidebase
- [Authorization Cheat Sheet, deny by default](https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html), OWASP
