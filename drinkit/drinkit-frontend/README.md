# drinkit-frontend

The Nuxt app of DrinkIt, rendered on the server. It extends [`nuxt-starter`](../../tech-starters/frontend/nuxt-starter),
which turns auto-imports off and sets the error page,
[`api-client-starter`](../../tech-starters/frontend/api-client-starter), which calls the backend through a client
generated from its contract, [`security-starter`](../../tech-starters/frontend/security-starter), which sets the
security headers and follows the backend's session: the sign-in by email code, the sign-out and the access each page
declares, and [`observability-starter`](../../tech-starters/frontend/observability-starter), which logs an unhandled
error with the backend's `traceId` and shows it in a toast of [Nuxt UI](https://ui.nuxt.com).

Every text comes from the [catalog](../../translations) at the root of the repository, through
[`i18n-starter`](../../tech-starters/frontend/i18n-starter): English at `/`, French under `/fr`, and a page for signed-in users without a prefix, in the language of the
account. The app loads the
prefixes its `nuxt.config.ts` names in `i18nCatalog`, and its tests check that every locale holds the keys of
`en.json`. The links of the `<head>` to the other languages take their origin from `i18n.baseUrl`, which a deployment
sets through `NUXT_PUBLIC_I18N_BASE_URL`: the built app refuses to start on a local origin.

The app replaces the error page of `nuxt-starter` with one of Nuxt UI, in `app/error.vue`, and shows a progress bar
while a navigation in the browser waits for its page. Without the backend, a page says that the server cannot be
reached: start it, then reload.

With the backend running (`./gradlew :drinkit-backend:bootRun`), sign in on `http://localhost:3000/sign-in` with the
code that Mailpit shows on `http://localhost:8025`.

From the root of the repository:

```bash
pnpm install --frozen-lockfile
pnpm --filter drinkit-frontend dev        # http://localhost:3000
pnpm --filter drinkit-frontend lint
pnpm --filter drinkit-frontend typecheck
pnpm --filter drinkit-frontend test
pnpm --filter drinkit-frontend build      # then node drinkit/drinkit-frontend/.output/server/index.mjs
```
