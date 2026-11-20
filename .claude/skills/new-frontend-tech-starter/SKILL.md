---
name: new-frontend-tech-starter
description: Creates a frontend tech starter for DrinkIt, one technical concern with no business code, shipped as a Nuxt layer and a package of the pnpm workspace under tech-starters/frontend that a Nuxt app, a -frontend module, extends. Use it when asked for a frontend tech starter, or when a technical concern of a Nuxt app, such as the API client setup, should live apart from the features.
paths:
  - "tech-starters/frontend/**"
---

# New frontend tech starter

A frontend tech starter does for a Nuxt app what a backend one does for a Spring application: it owns one
technical concern, knows no business word, and the app can take it or drop it (`tech-starters.md` under
`docs/content/en/1.engineering/2.architecture/`). ADR 023 records the layout, and the `README.md` of `nuxt-starter`
shows it on the first starter: follow both.

## 1. Check the concern

Say it in one sentence. If it needs a word of the domain it belongs in the app; if it needs two sentences it is two
starters. A new starter is a new module, a structuring choice: ask the maintainer first, as `AGENTS.md` says.

Then list what would change for a second app with its own backend: each of those values becomes an option of the
app (§3).

## 2. The layout

- A Nuxt layer in `tech-starters/frontend/<name>/`, its sources in the folders of a Nuxt app by where they run,
  `app/`, `server/`, `shared/` or `modules/`, as "The folders of a starter" of `frontend-architecture.md` says, and
  a package of the pnpm workspace named `@drinkit/<name>`, whose dependencies come from the catalog through the
  `dependency-change` skill.
- The app lists it in its `dependencies` as `workspace:*` and in the `extends` of its `nuxt.config.ts` by that
  name. The app wins over the layer.
- What the app imports from it goes through the `exports` of its `package.json`, such as
  `@drinkit/nuxt-starter/problem-detail`: auto-imports are off. The `exports` are its public API, the rest stays
  internal.
- It holds runtime code only. Lint rules it brings, such as a ban on bare strings for translations, go to
  `code-analysis/eslint`, where `drinkitEslint()` lives. `test-starter` is the one exception: a dev package, no
  layer, that the others list in their `devDependencies` for their tests.
- Its `eslint.config.ts` calls `drinkitEslint()` of `@drinkit/eslint-config` and its `.stylelintrc.json` extends
  `@drinkit/stylelint-config`, both dev dependencies, like the app's. Its `package.json` has the `lint` and
  `typecheck` scripts of `nuxt-starter`, which CI runs on every package.

## 3. The rules

- It never imports from the app, nor from the app's generated client: the app depends on the starter, never the
  reverse.
- It serves one app or several, from its first version, even while one app uses it. A value that belongs to one
  app, such as a backend URL, an API path, a contract, a name or a prefix, is an option the app sets in its own
  `nuxt.config.ts`, through the starter's module, never a constant of the starter. With no default that suits any
  app, the option is required and the build stops without it: the `apiClient` options of `api-client-starter` are
  the model.
- Its configuration sits under one `runtimeConfig` key of its own, which the layer's `nuxt.config.ts` declares with
  a default that suits any app, or empty for the module to fill, so that an environment variable overrides it per
  environment (`NUXT_PUBLIC_<KEY>_...` for the public part). Only what the browser may see goes in the public
  part. A text the starter shows the user, such as the message of a toast, goes in the layer's `app/app.config.ts`
  instead, which Nuxt merges with the app's: a build fixes it, no environment changes it.
- What it renders follows the `frontend-feature` skill.

## 4. What it also touches

- A `README.md` for the developer who uses it: what it provides, how the app extends it, each configuration key
  with its environment variable.
- The lanes of `.github/workflows/config/ci-lanes.yml` already cover `tech-starters/frontend/**`: nothing to add,
  unless the starter brings a folder another lane reads.

Its tests come first, through the `test-driven-development` skill, on the Vitest projects of `test-starter`, whose
README lists what to declare. The app's build builds the layer through
`extends`: it passes before the starter is done.
