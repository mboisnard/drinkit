---
title: "Engineering"
navigation:
  title: Introduction
description: "The order in which to read DrinkIt's engineering pages the first time, and which pages to keep for reference."
---

This part of the site explains how DrinkIt is built and why. Each page presents a practice, then how DrinkIt
applies it, with sources to go further. The application is a cellar manager for wines and spirits: a Kotlin and
Spring Boot backend, a Nuxt frontend, a PostgreSQL database. To run it, start with the
[quick start of the README](https://github.com/mboisnard/drinkit#quick-start). To change it,
[CONTRIBUTING.md](https://github.com/mboisnard/drinkit/blob/master/CONTRIBUTING.md) explains how a change reaches
master.

## 🧭 A path through the documentation

Read it in this order the first time. The sidebar follows the same order, and each page links to the next one at
its bottom. It starts with the [developer experience](./1.dev-exp.md): the two commands that run DrinkIt on your
machine, so that you can try what the next pages explain.

1. **The shape of the code.** The [monorepo](./2.architecture/1.monorepo.md) holds everything. The application is a
   [modular monolith](./2.architecture/2.modular-monolith.md) split by business area, each area built on the
   [hexagonal architecture](./2.architecture/3.hexagonal-architecture.md), and its technical concerns come from
   [tech starters](./2.architecture/6.tech-starters.md).
2. **Inside the domain.** Users are stored with [event sourcing](./3.domain/1.event-sourcing.md), and the parts of
   the application react to each other through [platform events](./3.domain/2.platform-events.md).
3. **Building and storing.** The [build tool](./4.build-and-data/1.build-tool.md) page explains Gradle and its conventions,
   the [database](./4.build-and-data/2.database.md) page how the schema changes without stopping the users.
4. **Writing code here.** The [best practices](./5.writing-code/1.best-practices/1.craft-manifesto.md) set the habits, the
   tests rely on [test doubles](./5.writing-code/2.testing/1.testdouble.md) and
   [Testcontainers](./5.writing-code/2.testing/2.testcontainers.md), and the
   [architecture tests](./5.writing-code/2.testing/4.architecture-tests.md) keep the rules of the steps above true on every
   build.
5. **With a coding assistant.** The [harness](./6.coding-assistants/1.harness.md) steers the coding agents and checks
   their work, through [hooks](./6.coding-assistants/2.hooks.md), [skills](./6.coding-assistants/3.skills/index.md) and
   [subagents](./6.coding-assistants/4.sub-agents.md).
6. **From a change to master.** The [CI](./7.delivery/1.ci-cd.md) runs what a change touches, and the master ruleset
   lets the maintainer merge a pull request only once its gate is green.
7. **Why it is this way.** The [decision records](../2.decisions/index.md) keep the reason of each structuring choice.

## 🗂️ For reference

The [Reference](../3.reference/index.md) tab holds the pages to look something up in rather than read in order:
the tech radars, security, the pages generated from the code and the ideas kept for later.
