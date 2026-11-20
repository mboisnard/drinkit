# drinkit-frontend

The Nuxt app of DrinkIt, rendered on the server. It extends [`nuxt-starter`](../../tech-starters/frontend/nuxt-starter),
which turns auto-imports off and sets the error page,
[`api-client-starter`](../../tech-starters/frontend/api-client-starter), which calls the backend through a client
generated from its contract, and [`security-starter`](../../tech-starters/frontend/security-starter), which sets the
security headers and follows the backend's session: the sign-in by email code, the sign-out and the access each page
declares.

With the backend running (`./gradlew :drinkit-backend:bootRun`), sign in on `http://localhost:3000/sign-in` with the
code that Mailpit shows on `http://localhost:8025`.

From the root of the repository:

```bash
pnpm install --frozen-lockfile
pnpm --filter drinkit-frontend dev        # http://localhost:3000
pnpm --filter drinkit-frontend lint
pnpm --filter drinkit-frontend typecheck
pnpm --filter drinkit-frontend build      # then node drinkit/drinkit-frontend/.output/server/index.mjs
```
