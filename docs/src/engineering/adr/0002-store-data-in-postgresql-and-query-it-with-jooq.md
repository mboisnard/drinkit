# 0002. Store data in PostgreSQL, and query it with jOOQ

- Status: accepted
- Date: 2026-10-24

DrinkIt stores its data in PostgreSQL, a relational database, and its adapters write their SQL with jOOQ rather than
behind an ORM. The data is made of relations that a schema can enforce, and every query stays visible as the SQL that
reaches the database.

## Context

DrinkIt's data is relational by nature: a user owns cellars, a cellar holds bottles, a bottle refers to a wine, a beer
or a spirit of the catalog, and a tasting gathers people around bottles. Those links have to hold, a bottle never in
a cellar that does not exist, and a change that touches several of them has to apply in full or not at all.

The project is also a place to understand what an application asks of its database. An ORM such as Hibernate turns
objects into SQL on its own: the query it sends, its joins, its lazy loads and its N+1 stay hidden until someone logs
them.

## Decision

DrinkIt keeps its data in PostgreSQL, as the [database page](../build-and-data/database.md) describes. The schema
enforces the links with constraints, and a change runs in a transaction. PostgreSQL also covers needs that could have
called for another system: the history of event-sourced aggregates, the
[browser sessions](0012-keep-sessions-in-postgresql.md), and JSON columns where a structure has to stay open.

The adapters query it with jOOQ. Its classes are generated from the schema, so a query that names a missing column
does not compile, and the code reads like the SQL it sends. An adapter that reads parents with their children, such
as cellars with their bottles, does it in one query with `MULTISET`, which nests the children in each parent's row,
and `jooq-kotlin` maps each level to a Kotlin constructor. No adapter writes a query per parent.

## Consequences

- Every query reads as SQL in the code, and its plan can be checked with `EXPLAIN` as written.
- A rename in the schema breaks the build at each query that uses the old name, once the classes are generated again.
- The code maps rows to the domain by hand: there is no entity, no dirty checking and no lazy loading to rely on.
- The generated classes follow the schema, so a schema change also regenerates them, against a running PostgreSQL.
- `MULTISET` carries the children as JSON inside PostgreSQL, so a decimal or a timestamp has to be read back and
  compared in the adapter's test.
- One more artifact, `jooq-kotlin`, follows the version of jOOQ.

## Alternatives

- **A document database such as MongoDB.** Rejected because DrinkIt's data is mostly links between users, cellars and
  bottles, which a document store neither constrains nor joins well. It offered an aggregate stored in one document.
- **JPA with Hibernate.** Rejected because it generates and hides the SQL, and lets the object model drive the schema,
  where DrinkIt wants to read and write the SQL it runs. It offered less mapping code.
- **Spring Data JDBC.** Rejected because a query beyond one aggregate falls back to SQL strings that no compiler
  checks. It offered a simpler model than JPA.

## Sources

- [jOOQ vs. Hibernate: When to Choose Which](https://blog.jooq.org/jooq-vs-hibernate-when-to-choose-which/), Lukas Eder
- [The MULTISET value constructor](https://www.jooq.org/doc/latest/manual/sql-building/column-expressions/multiset-value-constructor/), jOOQ
- [OrmHate](https://martinfowler.com/bliki/OrmHate.html), Martin Fowler, a defense of ORMs
