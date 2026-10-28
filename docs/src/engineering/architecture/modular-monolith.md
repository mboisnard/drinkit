# Modular monolith

A modular monolith is one application, deployed as a single unit, split inside into modules that each own a
business area behind clear boundaries. DrinkIt's backend is built this way: one application and one database, with
the bounded contexts as modules. The idea to keep: the simplicity of a monolith, with the separation of well-cut
modules. Read this page before you add a bounded context or make one context use another.

## 🎯 Why it helps

- **One deployment.** One artifact and one pipeline, no network between the parts of the application, and
  integration tests that run locally.
- **Simple consistency.** One process and one database: a use case writes in a single transaction, with no saga.
  Only what a [platform event](platform-events.md) triggers comes later, eventually consistent.
- **Thinking in business areas.** Each module owns a domain, such as users, cellars or bottles, with its own
  entities, use cases and interfaces. Domain-Driven Design applies from the first line of code.
- **Less to run while the product takes shape.** No distributed tracing, no security between services, no fault
  tolerance across a network.
- **A door left open.** A well-cut module can become a service of its own when a real need appears, and the cost
  of a distributed architecture is paid only then.

## 🧩 What makes a module

- **It owns one business area**: its data, its rules and its use cases, as a vertical slice.
- **It hides its insides** and shows a small contract: the types and interfaces other modules may use, and the
  events it publishes.
- **It talks to other modules through that contract only**: a call to one of its interfaces, or an event it
  publishes and others react to, without knowing who.
- **It never depends on a module that depends on it.** Two modules in a cycle can only change, or leave, together.
- **Inside, any architecture fits.** DrinkIt uses the [hexagonal architecture](hexagonal-architecture.md) in every
  module.

## 🍷 In DrinkIt

- **A module is a bounded context**, a package `com.drinkit.<context>`: `cellar`, `bottle`, `user`, `money`,
  `notification`. The same package name runs through the three Gradle modules of the hexagonal architecture, so a
  context reads as one slice across them. A shared `common` package holds the types every context uses.
- **Its contract** is its business types and its ports. When a context only needs to say that something happened,
  it publishes a [platform event](platform-events.md) that other parts of the application handle asynchronously,
  as the creation of a user triggers the verification email. Events travel inside the process today.
- **No cycle between contexts**, and `common` depends on none of them. The
  [architecture tests](../writing-code/testing/architecture-tests.md) check it on every build.
- **Extracting a context** would follow its boundaries: its packages become Gradle modules of their own, its
  interfaces get adapters that call the new service, events go through a message broker, and its tables move to a
  database of its own.

## ⚖️ What it costs

- **Boundaries hold by discipline.** Only cycles are checked. Anything else that ties two contexts, such as a query
  that joins their tables, passes the build, and reviews have to catch it.
- **Everything ships together.** A context cannot be deployed or scaled alone.

## 📚 Sources

- [Modular Monolith: A Primer](https://www.kamilgrzybek.com/blog/posts/modular-monolith-primer), Kamil Grzybek
- [Deconstructing the Monolith](https://shopify.engineering/deconstructing-monolith-designing-software-maximizes-developer-productivity), Kirsten Westeinde, Shopify Engineering
- [Microservices Killer: Modular Monolithic Architecture](https://medium.com/design-microservices-architecture-with-patterns/microservices-killer-modular-monolithic-architecture-ac83814f6862), Mehmet Ozkaya
- [Monolith First](https://martinfowler.com/bliki/MonolithFirst.html), Martin Fowler
- [Microservice Premium](https://martinfowler.com/bliki/MicroservicePremium.html), Martin Fowler
- [Strangler Fig Application](https://martinfowler.com/bliki/StranglerFigApplication.html), Martin Fowler
