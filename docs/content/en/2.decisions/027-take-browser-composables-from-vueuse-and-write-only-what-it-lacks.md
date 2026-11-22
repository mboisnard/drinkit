---
title: "027. Take the browser composables from VueUse, and write only what it lacks"
description: "A Nuxt app takes its browser and timing composables from VueUse through utils-starter, which lists where each common need is answered and holds the few helpers VueUse, Nuxt and the browser lack."
navigation:
  title: "027 · VueUse in utils-starter"
status: accepted
date: 2026-11-22
explainedIn:
  title: Frontend architecture
  to: /en/engineering/architecture/frontend-architecture
---

A Nuxt app of DrinkIt takes its browser and timing composables from VueUse, which
[`utils-starter`](https://github.com/mboisnard/drinkit/blob/master/tech-starters/frontend/utils-starter/README.md), a Nuxt layer, brings. It writes a helper of its own only for a need that VueUse, Nuxt and the browser leave open: a toolbox
grown in-house ends up copying a maintained library.

## Context

Every app soon needs the same small things: breakpoints that hold during the server render, a click outside a menu,
a debounced search, the clipboard. Written in-house, each one is code to test and keep up with the browsers, while
VueUse already offers them, tested against the server render.

VueUse, at 14.4.0 in November 2026, already reaches the app as a dependency of Nuxt UI, which pnpm keeps out of
the app's imports. Auto-imports are off in DrinkIt, as
[ADR 023](./023-render-the-frontend-with-nuxt-on-tech-starters-in-a-pnpm-workspace.md) records, so a composable is
always imported by name from a package the app depends on.

## Decision

`utils-starter`, a Nuxt layer the app extends, depends on `@vueuse/core` at the version of the pnpm catalog, so the
app imports it by name. Its [README](https://github.com/mboisnard/drinkit/blob/master/tech-starters/frontend/utils-starter/README.md)
lists the common needs and links each one to the function that answers it, in VueUse, Nuxt, Vue, the browser's [Baseline](https://web.dev/baseline) features or another
[tech starter](../1.engineering/2.architecture/6.tech-starters.md).

A helper of our own lands in the starter only when none of them answers, with its tests: `retry` for a call run
again after a wait, `useTextOverflow` for a text cut by an ellipsis. It never wraps a function of VueUse or Nuxt,
which would hide their documentation behind ours. `@vueuse/nuxt` is not installed, since its only gain is
auto-imports.

## Consequences

- A developer looks up a need in one table before writing code, and most needs cost an import.
- One more runtime dependency, whose majors land with [Renovate](https://docs.renovatebot.com/) and can rename or change a function the app uses.
- VueUse holds over two hundred functions and the catalog lists only the common needs: a need it misses sends the
  reader to VueUse's own list.

## Alternatives

- **An in-house toolbox.** Rejected because each helper copies a maintained function and needs its own tests. It
  offered full control of the API.
- **Only the catalog, with no dependency.** Rejected because the app would rely on the VueUse that Nuxt UI happens
  to bring, at a version it does not choose. It offered one less package.

## Sources

- [VueUse functions](https://vueuse.org/functions.html), VueUse
- [Nuxt layers](https://nuxt.com/docs/guide/going-further/layers), Nuxt
- [Composables](https://vuejs.org/guide/reusability/composables.html), Vue, the case for writing a small one yourself
