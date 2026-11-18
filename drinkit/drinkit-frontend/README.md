# drinkit-frontend

The Nuxt app of DrinkIt, rendered on the server. It extends [`nuxt-starter`](../../tech-starters/frontend/nuxt-starter),
which turns auto-imports off and sets the error page, and
[`security-starter`](../../tech-starters/frontend/security-starter), which sets the security headers.

From the root of the repository:

```bash
pnpm install --frozen-lockfile
pnpm --filter drinkit-frontend dev        # http://localhost:3000
pnpm --filter drinkit-frontend lint
pnpm --filter drinkit-frontend typecheck
pnpm --filter drinkit-frontend build      # then node drinkit/drinkit-frontend/.output/server/index.mjs
```
