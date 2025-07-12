# Modular Monolith

A **Modular Monolith** is a **monolithic application (single deployment)** structured into **logically independent modules** (functional domains, bounded contexts, or business features) while sharing the same process and storage.

It differs:
- From a spaghetti monolith by **strict module/domain separation**.
- From microservices by **not having physical separation** or distributed deployments.

## 🎯 Why use it?

### 1️⃣ Simplicity of Deployment
- **Single artifact to deploy**: reduces CI/CD friction in early stages.
- No premature network complexity or Kubernetes orchestration.
- Integration tests remain local and fast.

### 2️⃣ Maintain Transactional Consistency
- Easier to **handle ACID transactions** without requiring complex patterns like Saga or Eventual Consistency.

### 3️⃣ Promote Modular Architecture
- Forces you to **think in modules/domains** (e.g., `User`, `Cellar`, `Bottle`).
- Each module has its **own services, entities, and interfaces**, separating responsibilities.
- Allows you to apply **Domain-Driven Design (DDD)** from the start.

### 4️⃣ Prepare for scalability
- As the project grows, it is **easy to extract a module into a microservice** if needed.
- Reduces the migration cost to a distributed architecture **only when it becomes necessary**.

### 5️⃣ Reduce Initial Complexity
- Microservices require:
    - Distributed logging.
    - Fault tolerance management.
    - Advanced monitoring.
    - Inter-service security.
- These complexities are not needed at the start when your priority is to **validate the product and ship quickly**.

## 🛠️ Characteristics

✅ Domains are isolated and separated into clear modules.  
✅ Each module:
- Has its **own package/namespace**.
- May define **explicit interfaces**.
- Communicates with other modules only via clear interfaces.

✅ You can use:
- **Clean Architecture** or Hexagonal Architecture within each module.
- **CQRS within specific modules** if needed.
- A shared ORM (e.g., EF Core, Hibernate) but **encapsulated repositories within each module**.

::: tip
Many good articles to talk about this architecture:
- [Monolith First - Martin Fowler](https://martinfowler.com/bliki/MonolithFirst.html)
- [Modular Monolith: A Primer](https://www.kamilgrzybek.com/blog/posts/modular-monolith-primer)
- [Microservices Killer: Modular Monolithic Architecture](https://medium.com/design-microservices-architecture-with-patterns/microservices-killer-modular-monolithic-architecture-ac83814f6862)
:::