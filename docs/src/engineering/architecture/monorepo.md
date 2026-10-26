# Monorepo

A monorepo is a single repository that holds several distinct projects, with well-defined relationships between
them. DrinkIt keeps everything in one: its application, a backend and a frontend today, its
[tech starters](tech-starters.md), the build logic, the database changelogs, this documentation and the
[harness](../delivery/harness.md) that guides its coding agents. The idea to keep: a change and everything it affects travel
in one pull request, checked by one CI. Read this page before you add a folder at the root or a new project.

## 🎯 Why one repository

- **A change and its users land together.** A change to a shared piece updates the code that uses it in the same
  commit, and the compiler and the tests show the impact at once. A refactoring across projects is one pull
  request.
- **Nothing to publish internally.** Shared code is used straight from its folder, with no internal release to cut
  and no version to bump in each consumer.
- **One set of conventions and tools.** The build conventions, the code analysis, the CI and the dependency
  updates apply to every project, and an improvement reaches them all at once.
- **Everything in one place.** A newcomer finds the code, its documentation, its conventions and its deployment in
  one clone, and the documentation changes in the same pull request as the code it describes.

A monorepo is not a monolith: each project keeps its own boundaries, and the repository only puts them side by
side.

## 🗂️ What lives in the repository

| Folder | What it holds | Read more |
|---|---|---|
| `drinkit/` | The application: its domain, its adapters, its Spring Boot backend, its OpenAPI contract and its Nuxt frontend | [Hexagonal architecture](hexagonal-architecture.md) |
| `tech-starters/` | The reusable technical libraries the application builds on | [Tech starters](tech-starters.md) |
| `build-logic/`, `gradle/` | The build conventions, the version catalogs and the internal BOM | [Build tool](../build-and-data/build-tool.md) |
| `architecture-tests/`, `code-analysis/` | The architecture rules and the configuration of detekt | [Architecture tests](../writing-code/testing/architecture-tests.md) |
| `deployment/` | The services the application needs locally, and the database changelogs | [Database](../build-and-data/database.md) |
| `docs/` | This documentation site | [Living documentation](../writing-code/best-practices/living-documentation.md) |
| `.github/`, `.hooks/`, `.claude/` | The CI workflows, the git and agent hooks, the agent skills | [CI/CD](../delivery/ci-cd.md), [Harness](../delivery/harness.md) |

## ✅ Practices that keep it healthy

- **Projects stay apart.** An application never uses a module of another application, and a tech starter never
  uses a module of an application. There is one application today: the rule keeps the next one apart from it. The
  [architecture tests](../writing-code/testing/architecture-tests.md) check both, without which the repository slowly turns into one big
  project.
- **A new project joins by convention.** A folder directly under `drinkit/` or `tech-starters/backend/` that holds
  a build file joins the Gradle build on its own. The architecture rules find an application's modules by their
  suffix, such as `-domain`, so a new application is checked from its first build, as
  [ADR 0001](../adr/0001-architecture-tests-with-konture.md) records.
- **CI runs what a change touches.** Each lane of the [CI](../delivery/ci-cd.md) watches a set of paths: a change to the
  frontend skips the backend build. A path no lane knows runs them all, so a new folder at the root goes into the
  lane that builds it.
- **Caches keep the build short.** The Gradle build cache reuses the outputs of tasks whose inputs did not change,
  and the configuration cache skips the configuration of the build, as the [build tool](../build-and-data/build-tool.md) page
  explains.
- **Master changes through a pull request only**, merged once CI is green, since a change there reaches every
  project at once.
- **Unused code goes.** A module or a convention nobody uses is deleted rather than kept commented out: it would
  still build, still get dependency updates and still mislead the next reader.

## ⚖️ Limitations

- **A change can reach everything.** In DrinkIt, the build shows which projects a change breaks. With several
  teams, the change would also need their agreement before the merge.
- **The build grows with the repository.** CI lanes and caches keep it short today, but a full build still builds
  every project.
- **Very large monorepos need dedicated tooling**, as the version control and build system Google built for its
  own show. DrinkIt is far from that scale: Gradle and path-based CI lanes are enough.

## 📚 Sources

- [Monorepo Explained](https://monorepo.tools/), Nx
- [Advantages of monorepos](https://danluu.com/monorepo/), Dan Luu
- [Why Google Stores Billions of Lines of Code in a Single Repository](https://research.google/pubs/why-google-stores-billions-of-lines-of-code-in-a-single-repository/), Rachel Potvin and Josh Levenberg, Communications of the ACM
- [Monorepos: Please don't!](https://medium.com/@mattklein123/monorepos-please-dont-e9a279be011b), Matt Klein
- [Mostly Terrible: The Monorepo](https://medium.com/streamdal/mostly-terrible-the-monorepo-5db704f76bdb), Daniel Selans, Streamdal
