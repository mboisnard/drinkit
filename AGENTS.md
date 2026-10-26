# AGENTS.md

DrinkIt manages wine and spirit cellars: a Kotlin and Spring Boot 4 backend, a Nuxt frontend, PostgreSQL
through jOOQ. Exact versions live in the catalogs of `gradle/platform`, `gradle/gradle-daemon-jvm.properties`,
`.nvmrc` and `drinkit/drinkit-frontend/package.json`, not here.

## Commands

```
./gradlew build detektAll --continue           # what CI runs: compile, test, detekt
./gradlew :drinkit-domain:test                 # one module, named after its folder
./gradlew :drinkit-infra:test --tests '*JooqUserEventsTest'
./gradlew detektAll -Pdetekt.autoCorrect=true  # fixes formatting and auto-correctable findings
./gradlew :drinkit-backend:bootRun             # dev profile, starts deployment/local/compose.yml itself
./gradlew :architecture-tests:test             # the architecture rules, part of build
./gradlew :updater:check                       # the changelog rules and Squawk, part of build
```

The JDK of `gradle/gradle-daemon-jvm.properties` must be installed: Gradle finds it whatever `java` is on
the `PATH`, but never downloads it. Integration tests need Docker, and `./gradlew build` needs Node on the
`PATH`, at the version of `.nvmrc`, to run Squawk.

Nothing in the application creates the database schema: on a new database, apply the changelogs once, as
shown in [Database and jOOQ](#database-and-jooq), before using the app. From an IDE, running
`UpdaterApplication` in `deployment/updater` does the same through Liquibase.

Local URLs: API under `http://localhost:8080/drinkit/api/`, Swagger UI at `/drinkit/openapi/ui`, health at
`/drinkit/actuator/health`. Without a session, everything but `POST /api/auth/login` and
`POST /api/registration/new` answers 403: most of the API needs `ROLE_USER`, Swagger UI and actuator `ROLE_ADMIN`
(`SecurityConfiguration` in `drinkit-backend`). In dev, emails such as verification tokens are logged, not
sent.

Frontend, from `drinkit/drinkit-frontend`, with `java` on the `PATH` for the client generation:

```
npm ci
npm run generate:client-api   # TypeScript client from the OpenAPI contract
npm run dev                    # http://localhost:3000
npm run build
```

Documentation site: `./gradlew kspKotlin --rerun` first, which writes the generated pages, then from
`docs`: `npm ci && npm run docs:build`.

Compose file, workflows and Renovate configuration, from the root, with Docker:

```
docker compose -f deployment/local/compose.yml config --quiet
docker compose -f deployment/local/compose.yml up --detach --wait   # the dev services, until each healthcheck passes
docker run --rm -v "$PWD:/repo:ro" -w /repo "$(grep -o 'ghcr.io/[^ ]*actionlint[^ ]*' .github/workflows/ci-workflows.yml)"
docker run --rm -v "$PWD:/repo:ro" -w /repo ghcr.io/zizmorcore/zizmor --offline .
docker run --rm -v "$PWD:/repo:ro" -w /repo "$(grep -o 'ghcr.io/renovatebot/renovate[^ ]*' .github/workflows/ci-renovate.yml)" renovate-config-validator --strict --no-global .github/renovate.json
```

Hooks, from the root, with Node and `jq`: `npm cit --prefix .hooks` runs every bats suite under `sh`, and
`HOOK_SHELL=dash` before it runs the hooks under dash. ShellCheck, with Docker:

```
docker run --rm -v "$PWD:/mnt:ro" -w /mnt --entrypoint sh "$(grep -o 'koalaman/shellcheck[^ ]*' .github/workflows/ci-hooks.yml)" -c 'shellcheck .hooks/git/* .hooks/claude/* .hooks/lib/*.sh .hooks/tests/*.bats .hooks/tests/*.bash'
```

## Structure

- `drinkit/`: the application. `drinkit-domain` holds use cases, entities in `<context>/core` and ports in
  `<context>/spi`, shared ones in `common/spi`. `drinkit-infra` holds the adapters, `drinkit-backend` the Spring Boot application and
  its REST controllers, `drinkit-api-contract` the OpenAPI contract, `drinkit-frontend` the Nuxt app.
- `tech-starters/backend/`: one technical concern per starter, no business code.
- `build-logic/`: the `com.drinkit.*` convention plugins. A Kotlin module applies one archetype,
  `common-convention`, `library-convention` or `api-convention`, plus add-ons, and configures neither
  Kotlin, Java nor repositories itself.
- `gradle/platform/`: the version catalogs and the internal BOM.
- `deployment/local/compose.yml`: the local services the dev profile starts. `deployment/updater`: the
  schema, as Liquibase changelogs.
- `docs/src/engineering/`: the engineering documentation, its folders in the order of the site:
  `architecture/`, `build-and-data/`, `writing-code/`, `delivery/`, `adr/`. The sections below say which page
  to read before which change.
- `.hooks/`: the git hooks in `git/`, the Claude Code hooks in `claude/`, what they share in `lib/`, their bats
  suites in `tests/`.
- `architecture-tests/`: the architecture rules, as Konture tests.
  `docs/src/engineering/writing-code/testing/architecture-tests.md` lists each rule with its reason and fix,
  `docs/src/engineering/adr/` records the choices behind them, and `docs/src/engineering/delivery/harness.md`
  says where they run.
- `.claude/skills/`: how to carry out a kind of change, one folder each. Its `SKILL.md` says when it applies:
  read it before that change, whatever the agent. A skill names its model files by class: find one with
  `git grep -nwE '(class|interface|object) <Name>'`. `.claude/agents/`: the reviewers that check a branch.
- An agent's working files, drafts, test lists, reports, go under `git rev-parse --git-path claude/<file>`
  or in the session's scratch directory: never in the tree, never elsewhere on the machine.

A folder directly under `drinkit/` or `tech-starters/backend/` that holds a `build.gradle.kts` becomes a
Gradle project named after the folder.

## Code patterns

Read `docs/src/engineering/architecture/hexagonal-architecture.md` before adding a port or a module
dependency, and `docs/src/engineering/writing-code/best-practices/coding-rules.md` before naming things. The
architecture tests check the patterns of the domain, infra, backend, Spring, tech starters and tests, naming
included: `docs/src/engineering/writing-code/testing/architecture-tests.md` gives each rule with its reason and fix.

- Annotate new classes with the `documentation-starter` annotations. `@CoreDomain`, `@Usecase` and
  `@TechStarterTool` generate the documentation pages, the others mark a pattern in the code.
- Business values are types, never primitives. An input value exposes `validate()`, whose errors the use
  case, or its functional core, turns into a decision. An identifier or an invariant checks itself in `init`.
- The `User` aggregate is event-sourced: `UserEvents` stores its events, `Users` is the projection.
- Jackson 3: core, databind and the format modules are `tools.jackson.*`, but the generic annotations
  (`@JsonProperty`, `@JsonTypeInfo`…) stay in `com.fasterxml.jackson.annotation`.
- The existing tech starters are found by component scan and configured through a profile listed in
  `spring.profiles.include`. A new or migrated starter uses one `@AutoConfiguration` and no profile: follow
  `.claude/skills/new-backend-tech-starter/SKILL.md`.

## Tests

- JUnit Jupiter with Kotest assertions, names in backticks that state the behavior, `// Given`, `// When`,
  `// Then`. An architecture test only declares and checks its Konture rule.
- Never mock. Mockito comes with `spring-boot-starter-test` but stays unused: test doubles are written by
  hand in `src/testFixtures` (in-memory, spy, fake) and wired by `*Fixtures` classes.
- A port's behavior is written once, in an abstract `*TestContract` of the domain test fixtures. The
  in-memory double and the real adapter both extend it (`ExchangeRatesTestContract`).
- `@JooqIntegrationTest(schemas = [...])` takes the generated jOOQ schema class, not the Spring
  application. It starts PostgreSQL with Testcontainers and builds the schema from the committed jOOQ
  classes, not from Liquibase: a schema change is only tested once those classes are regenerated.

## Database and jOOQ

Read `docs/src/engineering/build-and-data/database.md` before writing a changelog. A schema change goes in a new
numbered file of `ddl/`, a data change in one of `dml/`, and `base/` changes only in a rebaseline, or before the
first deployment with the local database recreated. Every statement must be idempotent, which is what makes this
command safe to replay. It applies them all, in the order Liquibase runs them, and is also the first-run step:

```
docker compose -f deployment/local/compose.yml up -d postgresql
find deployment/updater/src/main/resources/db/changelog -name '*.sql' | sort | xargs cat | docker compose -f deployment/local/compose.yml exec -T postgresql psql -v ON_ERROR_STOP=1 -U drinkit -d drinkit
```

`./gradlew :updater:check` checks the changelogs: the rules of `database.md` and Squawk, each finding with its
rule and its fix. A Squawk rule that does not apply to a statement is tolerated in its file, with the reason on
the line above. `deployment/updater/.squawk.toml` lists every Squawk rule, on or off.

The jOOQ classes are committed under `src/generated/jooq/kotlin` in every module that applies
`com.drinkit.jooq-codegen-convention`, and never edited by hand. The build does not regenerate them: after a
changelog change, apply it, then run `./gradlew jooqCodegen`. It only generates the tables a module lists in
its `jooq` block `includes`, so a new table goes there first. Against a database without the schema, it
deletes the committed classes and still succeeds: check `git status` before committing.

## Dependencies

A version lives in a catalog of `gradle/platform` and in the BOM `gradle/platform/build.gradle.kts`.
Modules declare dependencies without a version, and a project cannot declare a repository.

Gradle checks every artifact against `gradle/verification-metadata.xml`, and `drinkit-backend` resolves
exactly what its `gradle.lockfile` lists. After adding or bumping a dependency, or adding a starter to the
application, regenerate both and review them:

```
./gradlew --write-verification-metadata sha256 --refresh-dependencies --write-locks dependencies
git diff gradle/verification-metadata.xml drinkit/drinkit-backend/gradle.lockfile
./gradlew build
```

Skipping it fails the build with "Dependency verification failed" or "not part of the dependency lock
state", or worse, leaves a bumped dependency silently at its locked version in the application. Keep
`--refresh-dependencies`: on a warm cache Gradle skips metadata that a fresh CI runner downloads, and CI
fails. To move a single dependency in the lock, use `--update-locks group:name` instead of `--write-locks`.
A verification failure on an untouched branch means an upstream release landed in a version range: the
same command fixes it.

## Build traps

- detekt fails the build on any finding, formatting included. Fix the code, and never add an entry to
  `code-analysis/detekt/baseline.xml`.
- The architecture tests fail the build on any broken rule: fix the code, or tolerate the violation in the
  rule's `suppress` block with the issue that removes it, never loosen the rule.
- Change the OpenAPI contract, never the code generated from it. A custom string format needs both
  `typeMappings` and `importMappings` in `drinkit/drinkit-backend/build.gradle.kts`.
- Read `docs/src/engineering/build-and-data/build-tool.md` before changing a convention. In `build-logic`, a
  plugin id is the file name: no `package` declaration, and file names unique across folders. The
  configuration cache is on, and every convention must stay compatible with it.

## Git and pull requests

- master only changes through a pull request with a green `CI gate`, merged by rebase. Resolve a conflict
  with a local rebase and `git push --force-with-lease`: GitHub's "Resolve conflicts" button adds a merge
  commit that blocks the merge.
- Run `git config core.hooksPath .hooks/git` once per clone: pre-commit formats the staged Kotlin, pre-push
  refuses a push to master.
- A branch is named `<issue>-<short-description>`. A commit subject reads `[TOPIC] Imperative subject
  (#issue)`, reusing a topic from `git log` when one fits, and the pull request title is the first commit
  subject. Group changes by topic, not by micro-step.
- Everything written on GitHub is in English: commits, pull requests, issues, comments.
- CI runs only the lanes a change touches, mapped in `.github/workflows/config/ci-lanes.yml`. A path no lane
  lists runs them all.
- A workflow references an action by commit SHA with its version as a comment,
  `uses: owner/action@<sha> # v1.2.3`, or one of this repository with `uses: $/.github/actions/<name>`.
- Merging a pull request, rulesets, branch protection and push protection bypasses are the maintainer's: a
  Claude Code hook refuses them, and any way around pre-push.
- `/implement-issue <issue>` takes an open issue to a pull request: its own worktree, test first, a fresh
  judge before the pull request is opened. Run again after the merge, it removes the worktree and pulls master.
- An issue's scope lists possibilities, not instructions. Analyze the current state first, and treat only
  the acceptance criteria as binding. The pull request explains the choices made.
- A structuring choice is the maintainer's: a new module or dependency, a change to the API contract, to the
  schema or to a security rule. Ask before making one: the facts with their real names, one question, two to
  four options of one line, your recommendation first. Once the maintainer settles it, the same pull request
  records it as an ADR, with the `new-adr` skill.
- A person reads every issue, pull request and comment: follow "Writing issues, pull requests and
  comments" in `CONTRIBUTING.md`. `gh` and the API skip the issue forms, so an issue follows the matching
  form in `.github/ISSUE_TEMPLATE/` by hand: each field's `label` as a `###` heading in the form's order, its
  prefilled `value` verbatim, an empty optional field left out, and the form's `title` prefix, `labels` and
  project passed to `gh issue create`. A pull request body follows `.github/pull_request_template.md`
  without its comments, and goes in with `--body-file`. It links its issue with `Closes #<issue>`, with
  `Part of #<issue>` when the issue stays open, or with `Follows up #<issue>` when it is already closed.

## Replies

Every answer to the maintainer in the terminal is in caveman mode, level full, from the first reply, in the
maintainer's language: answer first, no ceremony, articles optional, every fact, code span, path, number and
error kept verbatim.
Plain prose instead, then caveman again: a question to the maintainer, a security warning, an irreversible
action, steps whose order a fragment could scramble. `.claude/skills/caveman/SKILL.md` holds the rules and the
levels. "stop caveman" or "normal mode" switches it off. It never shapes what leaves the terminal: issues, pull
requests, comments and commits follow `CONTRIBUTING.md`.

## Code style

No comments by default. One or two lines at most, only for what the code cannot say: a trap, an external
constraint, why not the obvious way.

## This file

It holds rules and pointers, not inventories: a new module, starter or version should not need an edit
here. When a change makes one of its lines wrong, fix that line in the same pull request.
