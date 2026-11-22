# drinkit-frontend

The Nuxt app of DrinkIt, rendered on the server with [Nuxt UI](https://ui.nuxt.com), on the
[frontend tech starters](../../tech-starters/frontend) its `nuxt.config.ts` extends. Every text comes from the
[catalog](../../translations) at the root of the repository: English at `/`, French under `/fr`, and a page for
signed-in users without a prefix, in the language of the account. A deployment sets its public origin through
`NUXT_PUBLIC_I18N_BASE_URL`: the built app refuses to start on a local one.

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
