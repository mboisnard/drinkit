---
name: new-frontend-tech-starter
description: Creates a frontend tech starter for DrinkIt, one technical concern with no business code, shipped as a Nuxt layer under tech-starters/frontend that the Nuxt app extends. The first one settles its layout with the maintainer before any file is written. Use it when asked for a frontend tech starter, or when a technical concern of the Nuxt app, such as the API client setup, should live apart from the features.
---

# New frontend tech starter

A frontend tech starter does for the Nuxt app what a backend one does for the Spring application: it owns one
technical concern, knows no business word, and the app can take it or drop it (`tech-starters.md` under
`docs/src/engineering/guidelines/`). It is a Nuxt layer in `tech-starters/frontend/<name>/`, listed in the
`extends` of the app's `nuxt.config.ts` by a relative path: nothing to publish, and the app wins over it.

## 1. Settle the layout

Look for an existing frontend starter first: `ls tech-starters/frontend`. When one exists, its `README.md`
records the layout the maintainer chose: follow it, and go to step 2.

When none exists, this one sets the precedent, and a new module is a structuring choice. Before writing any file,
ask the maintainer, one question at a time:

1. The concern, in one sentence. If it needs a word of the domain it belongs in the app; if it needs two
   sentences it is two starters.
2. The folder name, and its sources at the root or under `src/` like the app.
3. Where its dependencies are installed: the app's `node_modules` is not above `tech-starters/frontend/`, and npm
   workspaces or a `package.json` of its own both change the install step of the frontend workflow.
4. How the app imports what it exposes when auto-imports are off: a relative path or an alias the layer declares.
5. Where the decision is written: the issue form asks for an ADR for a structuring choice.

The answers go in the starter's `README.md`, so that the next starter follows them.

## 2. The rules

- It never imports from the app, nor from its generated client `~/openapi`: the app depends on the starter,
  never the reverse.
- Its configuration sits under one `runtimeConfig` key of its own, with defaults in the layer's `nuxt.config.ts`,
  so that an environment variable overrides it (`NUXT_PUBLIC_<KEY>_...` for the public part). Only what the
  browser may see goes in the public part.
- What it renders follows the `frontend-feature` skill.

## 3. What the first one also touches

- The frontend lane of `.github/workflows/config/ci-lanes.yml`: without its folder there, a change in the layer
  matches no lane and CI runs every lane.
- The install step of `.github/workflows/ci-frontend.yml`, as answer 3 decided.
- The `paths:` of the skills that load on the app's frontend code, so that they load on the layer too:
  `grep -l '^  - ".*-frontend/src' .claude/skills/*/SKILL.md` lists them.
- A `README.md` for the developer who uses it: what it provides, how the app extends it, each configuration key
  with its environment variable.

Its tests come first, through the `test-driven-development` skill. The app's `npm run build` builds the layer
through `extends`: it passes before the starter is done.
