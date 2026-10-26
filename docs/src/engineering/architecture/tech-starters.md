# Tech starters

A tech starter brings one technical concern to an application, ready to use, and holds no business code. On the
backend it is a Spring Boot starter of our own, under `tech-starters/backend/`: an application adds the dependency
and gets the feature wired, with defaults it can override. On the frontend it is a Nuxt layer, and none exists
yet. The application's code says what DrinkIt does, the starters say with which technology. Read this page before
you create a starter or add one to an application.

DrinkIt has one application today, and the [generated pages](../resources/tech-starters/tech-starters.md) list its
backend starters. Those written so far predate the conventions below: they are wired by the application's
component scan and a Spring profile each. Copy their tests, not their wiring.

## 🎯 Why starters

- **The business code stays free of technical detail.** A secondary adapter of `drinkit-infra` uses
  `postgresql-starter` or `mail-starter`, and the domain never sees them, as the
  [hexagonal architecture](hexagonal-architecture.md) asks.
- **An application starts with its technical base ready.** DrinkIt has one application today. The next one gets
  its database access, security, messaging and monitoring from a dependency each, set up the same way. Spotify
  calls this a golden path, Chris Richardson a microservice chassis.
- **A technology changes in one place.** Replacing the mail server behind `mail-starter`, or the jOOQ and
  connection pool settings of `postgresql-starter`, moves no line of business code.

## 🧩 What makes a good starter

The rules below are the target of every backend starter, and the existing starters are being brought to them.

- **One concern, no business word.** A starter depends on no module of the application, `drinkit-domain`,
  `drinkit-infra` or `drinkit-backend`, and reads no table of a bounded context. A starter that needs two
  sentences to describe is two starters, so that an application can take one concern without the other.
- **Adding the dependency is enough.** One Spring Boot `@AutoConfiguration` does all the wiring, with no profile
  to turn on, and `starters.<name>.enabled=false` turns the starter off. A bean found by component scan cannot
  reliably back off, stays registered when the starter is off, and disappears in an application with another base
  package.
- **The application has the last word.** Every bean backs off when the application declares its own. Every
  setting has a default under `starters.<name>.*`, where `<name>` is the starter's name without `-starter`, such
  as `starters.postgresql.*`. An environment variable overrides each default, and the namespace, ours, never
  clashes with Spring's keys.
- **A small public API.** Everything but what an application injects is Kotlin `internal`, so no application
  depends on what the starter may change. `@TechStarterTool` marks the public part, and the
  [generated page](../resources/tech-starters/tech-starters.md) of each starter lists it.
- **Tests come with it.** The starter's test fixtures hold a [hand-written double](../writing-code/testing/testdouble.md), so that applications test
  against it without the real infrastructure. A test contract, passed by both the double and the real
  implementation, keeps the double faithful.
- **A README for the developer who uses it**: what the starter provides, and each setting with its environment
  variable.

In the [monorepo](monorepo.md), a starter has no version of its own: whoever changes a starter updates, in the same pull request,
the applications the change breaks, and the build shows them. The [architecture tests](../writing-code/testing/architecture-tests.md)
check the rules that show in the code, and the
[`new-backend-tech-starter` skill](https://github.com/mboisnard/drinkit/blob/master/.claude/skills/new-backend-tech-starter/SKILL.md)
gives the steps to write a starter.

## 🖥️ Frontend starters

On the Nuxt side, a starter is a [Nuxt layer](https://nuxt.com/docs/3.x/guide/going-further/layers): a partial
Nuxt application, with its configuration, composables and plugins, that the app lists in the `extends` of its
`nuxt.config.ts`. The app's own files win over the layer's. Good candidates are the concerns every page needs and
no feature owns: the setup of the HTTP client generated from the [OpenAPI contract](contract-first.md), the user's session, error
reporting and monitoring, translations. A frontend starter keeps its settings under one `runtimeConfig` key of its
own, which an environment variable overrides.

The first frontend starter sets the folder structure the next ones follow, as the
[`new-frontend-tech-starter` skill](https://github.com/mboisnard/drinkit/blob/master/.claude/skills/new-frontend-tech-starter/SKILL.md)
explains.

## ⚖️ What it costs

- **Shared code couples what uses it.** A change to a starter reaches every application at once. That is why
  starters stay technical: a shared database setup costs little to change, shared business rules would tie the
  applications together.
- **A convention to learn.** A developer has to know which starter brings what before writing their own
  configuration. The [generated pages](../resources/tech-starters/tech-starters.md) and the READMEs answer that.
- **Building for needs that do not exist.** A starter nobody uses still costs its upkeep, so it is extracted from
  a concern an application already has.
- **One set per stack.** A concern both sides need, such as monitoring, takes a Spring starter and a Nuxt layer.

## 📚 Sources

- [Pattern: Microservice chassis](https://microservices.io/patterns/microservice-chassis.html), Chris Richardson
- [How We Use Golden Paths to Solve Fragmentation in Our Software Ecosystem](https://engineering.atspotify.com/2020/08/how-we-use-golden-paths-to-solve-fragmentation-in-our-software-ecosystem), Gary Niemen, Spotify Engineering
- [What I Talk About When I Talk About Platforms](https://martinfowler.com/articles/talk-about-platforms.html), Evan Bottcher
- [Don't Share Libraries among Microservices](https://www.phauer.com/2016/dont-share-libraries-among-microservices/), Philipp Hauer
- [Creating Your Own Starter](https://docs.spring.io/spring-boot/reference/features/developing-auto-configuration.html#features.developing-auto-configuration.custom-starter), Spring Boot reference documentation
- [Authoring Nuxt Layers](https://nuxt.com/docs/3.x/guide/going-further/layers), Nuxt documentation
