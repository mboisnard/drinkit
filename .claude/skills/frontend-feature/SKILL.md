---
name: frontend-feature
description: Shapes a feature of DrinkIt's Nuxt frontend the way the app is built, calling the backend only through the client generated from the OpenAPI contract and leaving business rules to the backend. Use it before adding or changing code of the Nuxt app, drinkit-frontend.
paths:
  - "drinkit/*-frontend/src/**"
  - "drinkit/*-frontend/nuxt.config.ts"
---

# A feature in the Nuxt frontend

The frontend shows what the backend decides. Read the app's `package.json` and `nuxt.config.ts` before applying
a Nuxt habit: they say what exists and how the app sets it, auto-imports, PrimeVue, tooling. Tests come first,
through the `test-driven-development` skill.

## Where the code goes

Pages compose components and make the calls. Components display their props and emit what the user does. Logic,
state and the mapping of a response live in composables or plain functions, where a test can reach them: a
template holds no more than a condition or a loop.

## Calling the backend

- Only through the client generated into `src/openapi/`, imported from `~/openapi`: its `*Api` classes for the
  calls, its types for requests and responses. No hand-written `fetch`, `$fetch` or `useFetch` to the API, and
  no hand-written type that copies a schema of the contract.
- A feature that needs a new operation or field changes the contract first: the `api-contract-change` skill.
- On an error status, a generated call throws a `ResponseError` that carries the response. Catch it where the
  page can tell the user what happened.
- No workaround in a page, such as a hard-coded URL. A client configuration that several pages share is a
  frontend tech starter: the `new-frontend-tech-starter` skill.
- The session is the backend's http-only `SESSIONID` cookie: the frontend never reads, stores or sends a session
  token itself, and keeps nothing secret in `localStorage`.

## Business rules stay in the backend

The backend decides validation, permissions and computed values. The frontend shows the outcome, the response or
the error. A form may help typing, a required field left empty for instance, but the backend's answer is what
the user sees. When a page needs a decision the API does not give, the contract changes, not the page.

## Display

- PrimeVue components rather than hand-made ones, and PrimeIcons for icons. Read the PrimeVue version in
  `package.json` before reading its documentation.
- No `v-html` on content from the API or from a user: interpolation escapes, `v-html` does not. Rendering HTML
  needs a sanitizer the maintainer agrees on.
- Accessible markup, which is also what tests select by: a label tied to each input, an `aria-label` on an
  icon-only button, a `Button` or a link for an action.

## Checks

The frontend commands of `AGENTS.md`. The build does not check types, so a type error can pass it: also run
every check script `package.json` defines, type check, linter or tests, whichever exist.
