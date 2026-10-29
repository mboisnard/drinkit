# Ideas to explore

Links kept to look at later. None of them is planned work: an idea that becomes work gets an issue on the
[DrinkIt Roadmap](https://github.com/users/mboisnard/projects/2).

## Similar applications

- [Akiani, a wine and spirit cellar application](https://www.akiani.fr/realisations/application-de-gestion-de-caves-a-vins-et-spiritueux/)
- Vivino
- Wine-Searcher
- Ploc

## Wine, spirit and cocktail data

Sources for the importers and the scrapers of the domain mind map.

- [RapidAPI, beer, wine and alcohol APIs](https://rapidapi.com/blog/best-beer-wine-alcohol-api/)
- [TheCocktailDB on RapidAPI](https://rapidapi.com/thecocktaildb/api/the-cocktail-db)
- [viviner, a Vivino scraper](https://github.com/gugarosa/viviner)

## Engineering

Practices DrinkIt has none of yet, to weigh when a need for them appears.

- An application generator, first a skill and then a script, that creates the `-domain`, `-infra`, `-backend`,
  `-frontend` and `-api-contract` modules of a new application, with its starters, its security configuration and a
  sample contract. A reference application that CI regenerates and builds keeps the generator from rotting.
- A test of the access rules generated with each application: an anonymous call to `/api/**` is refused, a public
  path is open, an unknown path is denied, an admin path needs its role.
- Rules for an API exposed to others: the version in the path, two live versions at most, and the
  [Deprecation](https://www.rfc-editor.org/rfc/rfc9745) and [Sunset](https://www.rfc-editor.org/rfc/rfc8594) headers,
  with a security chain and a documentation of its own.
- An [idempotency key header](https://datatracker.ietf.org/doc/draft-ietf-httpapi-idempotency-key-header/) on the
  unsafe methods, so that a double click does not create two cellars.
- How-to pages whose code is copied from a compiled test, so that their examples cannot drift.
- A measure of which skill an agent loads for sample prompts: the right one, and no wrong one.
- A ratchet in the architecture tests: a kind of module that may gain no new dependency.

## Gradle plugins

- [GitHub dependency graph plugin](https://github.com/gradle/github-dependency-graph-gradle-plugin), which the
  CI's dependency submission already runs
- [Allure Gradle plugin](https://github.com/allure-framework/allure-gradle), for test reports
- [IDEA settings plugin](https://github.com/remal-gradle-plugins/idea-settings), to share IntelliJ settings

## JDK tools

- `jlink`, to build a smaller runtime for the images
- `jdeps`, to list the JDK modules the application needs

## Splitting bounded contexts

How a model that two contexts share splits once their rules differ, and how the contexts talk afterwards.

- [Model Mitosis](https://gitlab.com/beyondxscratch/model-mitosis), Julien Topçu and Josian Chevalier, a booking
  model split into search, booking and money, one commit per step
- [Model Mitosis: dealing with model tensions](https://youtu.be/VO6BSb52K5g), the talk at DDD Europe 2023, and
  [its French version at Devoxx France](https://youtu.be/HNQJW5iMZgQ)
- [Heuristics of model tensions](https://gitlab.com/beyondxscratch/model-tension-heuristics), the signs that a
  model should split
- [The Hive pattern](https://gitlab.com/beyondxscratch/hive-pattern), a module per bounded context, which calls
  another through a port of its own
