---
title: "026. Translate from one ICU catalog the frontend and the backend share"
description: "Every text a user reads lives in one flat JSON file per language at the root of the repository, in ICU MessageFormat, from which each app and starter loads the key prefixes it declares, through an i18n-starter at the base of the frontend starters."
navigation:
  title: "026 · One ICU catalog"
status: accepted
date: 2026-11-21
explainedIn:
  title: Frontend architecture
  to: /en/engineering/architecture/frontend-architecture
---

Every text a user reads lives in one catalog at the root of the repository, `translations/en.json` and
`translations/fr.json`, written in ICU MessageFormat, which the frontend's formatjs and the backend's ICU4J both read.
Each app and starter loads only the keys of the prefixes it declares, such as `cellars` for `cellars.empty`, through
`i18n-starter`, which every frontend starter now builds on.

## Context

The app and its starters wrote their texts in English in the templates and in each starter's `app.config.ts`. The
backend writes texts too, the emails and the titles of the ProblemDetail its API answers, always in French. A user should read one
language everywhere, and a translator should find every text in one place.

Two syntaxes compete. vue-i18n's own has no implementation on the JVM. ICU MessageFormat 1 is a standard with a mature
implementation on each side, formatjs and ICU4J, and its plural rules come from the CLDR, so French counts 0 as
singular where English does not. Nothing pays for a translation management system: a language model writes the
French with the English, in the same pull request.

`@nuxtjs/i18n` 10.6 compiles a JSON locale file at build with vue-i18n's syntax, and that build fails on an ICU
plural. It leaves a `.ts` loader alone, which is how `i18n-starter` hands it the messages.

## Decision

**One catalog, one flat JSON file per language**, named by its language code, keys such as `cellars.empty`. The first
segment of a key is its prefix. An app and each starter it extends declare in their `nuxt.config.ts` the prefixes they
show, and only those reach the browser, so that a page ships no text it never shows, such as an email. `en` is the source, every other file holds the same keys with the same arguments, and a test of
the app checks it.

**ICU MessageFormat 1, in the subset both sides read the same.** Plain and number arguments, `plural`, `select` and
`selectordinal`. No skeleton, offset, tag nor date: `d()` and `n()` format a date or an amount before it enters a
message. The typographic apostrophe `’` only, since the ASCII one escapes in ICU.

**`i18n-starter` at the base of the frontend starters.** It wraps `@nuxtjs/i18n`, compiles the messages with
`intl-messageformat`, and gives `useTranslate()`, whose `t` takes only a key of the catalog the app loads, with the values its message places.
`nuxt-starter` extends it, so the error page and every starter translate. The `$t` of vue-i18n and its `useI18n` fail
the lint, since they take any string.

**A public page in English at `/`, and in French under `/fr`.** The URL decides the language of a page a visitor
may open, not the browser, so that it renders the same for every request and a search engine indexes each language.

**A page for signed-in users without a prefix, in the language of the account**, which `GET /api/user/me` answers
in `locale`. `security-starter` takes the prefix off every page whose `auth` is not `public`: no page chooses. The
account's language wins at sign-in, and a visit without a session starts again from the URL: no cookie keeps a
language. Each call to the backend names the language of the page in `Accept-Language`.

**The backend reads the same files** for what it writes itself, the emails and the titles of a ProblemDetail, under
prefixes no app declares, such as `mail.` and `problem.`. An error the user reads in the app comes back as a status,
which the app translates.

## Consequences

- A text added to a page adds its key to both files in the same change, and the type check refuses a key the
  catalog lacks.
- A starter's texts sit beside the app's: a second app reuses a starter's translations by declaring its prefix.
- The flat ICU JSON is formatjs's "simple JSON", which Phrase, Lokalise, Tolgee and Locize import and export as is: a
  translation management system can take over the values later, and the checks stay in CI.
- Every frontend starter depends on `@nuxtjs/i18n`, whose version the documentation site's theme, Docus, also pins:
  a bump of one waits for the other.
- vue-i18n marks a custom message compiler as experimental: a release that changes it changes `i18n-starter`.
- A translation written by a language model is not proofread by a native speaker.
- `locale` is `fr` for every account until the user projection carries the language of its account (#707).

## Alternatives

- **One catalog per layer, in each starter.** Rejected because the backend would have no file to read, and a text
  shared by two layers would live twice. It offered a starter that carries its texts.
- **vue-i18n's syntax, and `.properties` for the backend.** Rejected because the same message would be written twice,
  in two syntaxes that count plurals differently. It offered `@nuxtjs/i18n`'s precompiled JSON with no code of ours.
- **French by default, at `/`.** Rejected because English is the source language of the catalog and of every page
  written so far. It offered the language of the first users.
- **A prefix on every page, signed-in ones included.** Rejected because nothing indexes a private page, and a link
  to `/cellars` should open in the language of whoever follows it. It offered one rule for every page.
- **A page that declares itself without a prefix.** Rejected because a page for signed-in users could keep one by
  mistake. It offered a choice per page.
- **A cookie that keeps the account's language after sign-out.** Rejected for now: a sign-in page in the user's
  language after their session expired is not worth a second source of the language. It offered that continuity.
- **Each app and starter calls `@nuxtjs/i18n` itself.** Rejected because the error page of `nuxt-starter` must
  translate too, and each would load the catalog its own way. It offered one layer fewer.

## Sources

- [ICU MessageFormat](https://unicode-org.github.io/icu/userguide/format_parse/messages/), Unicode
- [Message syntax](https://formatjs.github.io/docs/core-concepts/icu-syntax), formatjs
- [Lazy-load translations](https://i18n.nuxtjs.org/docs/guide/lazy-load-translations), Nuxt i18n
