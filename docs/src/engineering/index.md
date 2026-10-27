# Engineering

This part of the site explains how DrinkIt is built and why. Each page presents a practice, then how DrinkIt
applies it, with sources to go further. The application is a cellar manager for wines and spirits: a Kotlin and
Spring Boot backend, a Nuxt frontend, a PostgreSQL database. To run it, start with the
[quick start of the README](https://github.com/mboisnard/drinkit#quick-start). To change it,
[CONTRIBUTING.md](https://github.com/mboisnard/drinkit/blob/master/CONTRIBUTING.md) explains how a change reaches
master.

## 🧭 A path through the documentation

Read it in this order the first time. The sidebar follows the same order, and each page links to the next one at
its bottom.

1. **The shape of the code.** The [monorepo](architecture/monorepo.md) holds everything. The application is a
   [modular monolith](architecture/modular-monolith.md) split by business area, each area built on the
   [hexagonal architecture](architecture/hexagonal-architecture.md), and its technical concerns come from
   [tech starters](architecture/tech-starters.md).
2. **Inside the domain.** Users are stored with [event sourcing](architecture/event-sourcing.md), and the parts of
   the application react to each other through [platform events](architecture/platform-events.md).
3. **Building and storing.** The [build tool](build-and-data/build-tool.md) page explains Gradle and its conventions,
   the [database](build-and-data/database.md) page how the schema changes without stopping the users.
4. **Writing code here.** The [best practices](writing-code/best-practices/craft-manifesto.md) set the habits, the
   tests rely on [test doubles](writing-code/testing/testdouble.md) and
   [Testcontainers](writing-code/testing/testcontainers.md), and the
   [architecture tests](writing-code/testing/architecture-tests.md) keep the rules of the steps above true on every
   build.
5. **With a coding assistant.** The [harness](coding-assistants/harness.md) steers the coding agents and checks
   their work, through [hooks](coding-assistants/hooks.md), [skills](coding-assistants/skills.md) and
   [subagents](coding-assistants/sub-agents.md).
6. **From a change to master.** The [CI](delivery/ci-cd.md) runs what a change touches, and the master ruleset
   lets the maintainer merge a pull request only once its gate is green.
7. **Why it is this way.** The [decision records](adr/index.md) keep the reason of each structuring choice.

## 🗂️ For reference

- **[Tech radars](tech-radars/index.md)**: the technologies and practices in use, and how much we trust each.
- **[Security](security/stateful-stateless.md)**: sessions, CSRF, CORS, headers and the protection of the login.
- **Generated pages**: the [business domains](resources/domains/domains.md) and the
  [tech starters](resources/tech-starters/tech-starters.md), written from the code by the
  [living documentation](writing-code/best-practices/living-documentation.md).
- **[Ideas](ideas.md)**: links kept to explore later.
