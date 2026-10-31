# Developer experience

Two commands start DrinkIt on a developer's machine: one for the backend, one for the frontend. The backend starts
the containers it needs and fills the database when it has to, so a fresh clone needs no other step. Read this
page before you change how DrinkIt starts locally.

## 🚀 Two commands

```
./gradlew :drinkit-backend:bootRun                 # the containers, the database, then the API on localhost:8080
cd drinkit/drinkit-frontend && npm run dev         # the app on localhost:3000
```

Before its first start, the frontend needs `npm ci` and `npm run generate:client-api`, with `java` on the `PATH`.

- **The database fills itself.** Each start of the backend applies the changelogs that the database has not
  received yet, then the demo data. A new database gets its schema, and a changelog you pull reaches yours at the
  next start. A changelog that fails stops the backend with Liquibase's error.
- **An account is ready to log in.** `demo@drinkit.test`, with the password `Drinkit-demo1`, is an admin of the
  local database. [ADR 0005](../../adr/0005-demo-data-from-a-local-only-changelog.md) records why the demo data
  never reaches a deployed database.

## 🔓 Values public on purpose

These values are in the repository because they only exist locally. Any other credential comes from an environment
variable:

- the `drinkit` role and its password `admin`, and the Meilisearch key `masterKey`, in `deployment/local/compose.yml`
  and the `application-dev.yml` files
- the demo account's password, `Drinkit-demo1`
