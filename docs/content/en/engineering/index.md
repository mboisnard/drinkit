---
title: "Engineering"
description: "This part of the site explains how DrinkIt is built and why."
---

This part of the site explains how DrinkIt is built and why. Each page presents a practice, then how DrinkIt
applies it, with sources to go further. The application is a cellar manager for wines and spirits: a Kotlin and
Spring Boot backend, a Nuxt frontend, a PostgreSQL database. To run it, start with the
[quick start of the README](https://github.com/mboisnard/drinkit#quick-start). To change it,
[CONTRIBUTING.md](https://github.com/mboisnard/drinkit/blob/master/CONTRIBUTING.md) explains how a change reaches
master.

## 🧭 A path through the documentation

Read it in this order the first time. The sidebar follows the same order, and each page links to the next one at
its bottom.

1. **The shape of the code.** The [monorepo](./1.architecture/1.monorepo.md) holds everything. The application is a
   [modular monolith](./1.architecture/2.modular-monolith.md) split by business area, each area built on the
   [hexagonal architecture](./1.architecture/3.hexagonal-architecture.md), and its technical concerns come from
   [tech starters](./1.architecture/6.tech-starters.md).
2. **Inside the domain.** Users are stored with [event sourcing](./1.architecture/7.event-sourcing.md), and the parts of
   the application react to each other through [platform events](./1.architecture/8.platform-events.md).
3. **Building and storing.** The [build tool](./2.build-and-data/1.build-tool.md) page explains Gradle and its conventions,
   the [database](./2.build-and-data/2.database.md) page how the schema changes without stopping the users.
4. **Writing code here.** The [best practices](./3.writing-code/1.best-practices/1.craft-manifesto.md) set the habits, the
   tests rely on [test doubles](./3.writing-code/2.testing/1.testdouble.md) and
   [Testcontainers](./3.writing-code/2.testing/2.testcontainers.md), and the
   [architecture tests](./3.writing-code/2.testing/4.architecture-tests.md) keep the rules of the steps above true on every
   build.
5. **With a coding assistant.** The [harness](./4.coding-assistants/1.harness.md) steers the coding agents and checks
   their work, through [hooks](./4.coding-assistants/2.hooks.md), [skills](./4.coding-assistants/3.skills.md) and
   [subagents](./4.coding-assistants/4.sub-agents.md).
6. **From a change to master.** The [CI](./5.delivery/1.ci-cd.md) runs what a change touches, and the master ruleset
   lets the maintainer merge a pull request only once its gate is green.
7. **Why it is this way.** The [decision records](./6.adr/index.md) keep the reason of each structuring choice.

## 🗂️ For reference

- **[Tech radars](./7.tech-radars/index.md)**: the technologies and practices in use, and how much we trust each.
- **[Security](./8.security/1.principles.md)**: sessions, cookies, CSRF, headers and sign-in, and
  [how to call the API](./8.security/2.calling-the-api.md).
- **Generated pages**: the [business domains](./9.resources/domains/index.md) and the
  [tech starters](./9.resources/tech-starters/index.md), written from the code by the
  [living documentation](./3.writing-code/1.best-practices/5.living-documentation.md).
- **[Ideas](./10.ideas.md)**: links kept to explore later.
