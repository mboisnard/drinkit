---
name: frontend-feature
description: Shapes a feature of DrinkIt's Nuxt frontend the way the app is built, rendered on the server with explicit imports, calling the backend only through a client generated from the OpenAPI contract and leaving business rules to the backend. Use it before adding or changing code of a Nuxt app, a -frontend module, or of a frontend tech starter.
paths:
  - "**/*-frontend/app/**"
  - "**/*-frontend/nuxt.config.ts"
  - "tech-starters/frontend/**"
  - "code-analysis/eslint/**"
  - "code-analysis/stylelint/**"
---

# A feature in the Nuxt frontend

The frontend shows what the backend decides. A Nuxt app extends `nuxt-starter` and the other frontend tech
starters: read the app's `package.json` and `nuxt.config.ts` before applying a Nuxt habit, they say what exists.
The rules of the lint and their reasons are on the "Frontend coding conventions" page under
`docs/content/en/1.engineering/5.writing-code/`. Tests come first, through the `test-driven-development` skill.

## Where the code goes

Pages compose components and make the calls. Components display their props and emit what the user does. Logic,
state and the mapping of a response live in composables or plain functions, where a test can reach them: a
template holds no more than a condition or a loop. A concern with no business word, used by several pages, is a
frontend tech starter: the `new-frontend-tech-starter` skill.

## Explicit imports

Auto-imports are off. Every name comes from an import: Vue from `vue`, Nuxt composables from `#imports` or `#app`,
components from `#components`, a starter from its `@drinkit/<name>` package. The lint refuses a component used
without its import, the type check a composable.

## Rendered on the server

Each page renders on the server first, then in the browser. Code that runs while a component sets up runs on both:

- State shared across the app goes through `useState`, never a `ref` at module level, which every request the
  server renders would share.
- A browser global, `window`, `document` or `localStorage`, is read in `onMounted`, under `import.meta.client`, or
  in an event handler.
- A composable is not `async`: after its first `await` it has lost its component. It returns a ref it fills, or
  uses `useAsyncData`.

## Calling the backend

- Only through the functions of `#api`, which `api-client-starter` generates from the OpenAPI contract: no
  hand-written `fetch`, `$fetch` or `useFetch` to the API, and no hand-written type that copies a schema of the
  contract.
- Each call takes `useApi()` of `@drinkit/api-client-starter/use-api`, read in the composable before any `await`,
  and a read runs in `useAsyncData`: the call then carries the cookies of its own request, and a server render
  hands the browser the cookies the backend set.
- A page maps a response to what it shows only where the two differ, in a plain function next to the composable.
- A feature that needs a new operation or field changes the contract first: the `api-contract-change` skill.
- An error the backend answers throws a `ProblemError`, which carries its ProblemDetail. Catch it where the page
  can tell the user what happened; `error.vue` of `nuxt-starter` shows the ones nobody catches.
- The session is the backend's http-only session cookie: the frontend never reads, stores or sends a session
  token itself, and keeps nothing secret in `localStorage`.

## Business rules stay in the backend

The backend decides validation, permissions and computed values. The frontend shows the outcome, the response or
the error. A form may help typing, a required field left empty for instance, but the backend's answer is what
the user sees. When a page needs a decision the API does not give, the contract changes, not the page.

## Display

- The components of the design system the app extends, read from its `package.json`, rather than hand-made ones.
- No `v-html`: interpolation escapes, `v-html` does not, and the lint refuses it.
- Accessible markup, which is also what tests select by: a label tied to each input, an `aria-label` on an
  icon-only button, a button or a link for an action.

## Checks

From the root, for the app or the starter changed, `<package>` being its `name` in `package.json`:

```bash
pnpm --filter <package> lint
pnpm --filter <package> typecheck
pnpm --filter <package> build    # an app: a starter builds through the app that extends it
```
