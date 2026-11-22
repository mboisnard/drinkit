# Nuxt Starter

A frontend [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/content/en/1.engineering/2.architecture/6.tech-starters.md)
that every Nuxt app of DrinkIt extends: explicit imports, the error page, the browsers it builds for and the hooks
the other starters share. It extends [`i18n-starter`](../i18n-starter/README.md), so the app and every starter
translate their texts. The security headers come from [`security-starter`](../security-starter/README.md). It is
a [Nuxt layer](https://nuxt.com/docs/guide/going-further/layers), published in the pnpm workspace as
`@drinkit/nuxt-starter`.
[ADR 023](https://github.com/mboisnard/drinkit/blob/master/docs/content/en/2.decisions/023-render-the-frontend-with-nuxt-on-tech-starters-in-a-pnpm-workspace.md)
records why.

```mermaid
flowchart LR
    App[drinkit-frontend] -- extends --> Starter["@drinkit/nuxt-starter"]
    Starter -- extends --> I18n["@drinkit/i18n-starter"]
    App -- extends --> Security["@drinkit/security-starter"]
    App -- extends --> Observability["@drinkit/observability-starter"]
```

## Usage

```json
{
  "dependencies": {
    "@drinkit/nuxt-starter": "workspace:*"
  }
}
```

```ts
// nuxt.config.ts
import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: ['@drinkit/nuxt-starter'],
});
```

Every Vue API and composable then comes from an import. It gives:

- **The error page**, `app/error.vue`, whose button leads to the home page in the language of the page, under the key
  `nuxt.error.backHome` of the catalog.
- **`toProblemDetail()`**, from `@drinkit/nuxt-starter/problem-detail`: the ProblemDetail an error carries, with the
  backend's `traceId`, or its status alone, which `app/error.vue` shows.
- **The hooks** of `app/hooks.d.ts`, typed: `drinkit:api:problem`, which `api-client-starter` calls, `drinkit:error`,
  which `observability-starter` calls for an error nobody handled, `drinkit:signed-in` and `drinkit:signed-out`,
  which `security-starter` calls. A starter calls them, another listens, and a wrong name fails the type check.

## Configuration

| Setting | Effect |
|---|---|
| `imports.autoImport: false`, from `i18n-starter` | No auto-import. Nuxt reads `components` per layer, so the app sets `components: { dirs: [] }` itself for its own components |
| `routeRules` | A year of cache on the hashed files of `/_nuxt/` |
| `typescript.tsConfig`, `erasableSyntaxOnly` | No `enum`, namespace nor parameter property: the compiler refuses TypeScript it cannot strip to JavaScript |
| `vite.build.target` | The browsers of `.browserslistrc`, which `eslint-plugin-compat` checks the code against |

An app overrides any of them in its own `nuxt.config.ts`: the app wins over the layer.

## Tests

The tests run on the Vitest projects of [`test-starter`](../test-starter/README.md). The lint rules live apart, in
[`code-analysis/eslint`](https://github.com/mboisnard/drinkit/blob/master/code-analysis/eslint/README.md).
