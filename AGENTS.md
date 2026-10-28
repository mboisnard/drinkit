# AGENTS.md

DrinkIt manages wine and spirit cellars: a Kotlin and Spring Boot 4 backend, a Nuxt frontend, PostgreSQL
through jOOQ. Exact versions live in the catalogs of `gradle/platform`, `gradle/gradle-daemon-jvm.properties`,
`.nvmrc` and `drinkit/drinkit-frontend/package.json`, not here.

## Commands

```
./gradlew build detektAll --continue           # what CI runs: compile, tests, architecture rules, changelogs, detekt
./gradlew :drinkit-domain:test                 # one module, named after its folder
./gradlew :drinkit-infra:test --tests '*JooqUserEventsTest'
./gradlew detektAll -Pdetekt.autoCorrect=true  # fixes formatting and auto-correctable findings
./gradlew :drinkit-backend:bootRun             # dev profile: starts the compose services, logs emails instead of sending
git config core.hooksPath .hooks/git           # once per clone: pre-commit formats Kotlin, pre-push refuses master
```

Frontend, from `drinkit/drinkit-frontend`, with `java` on the `PATH` for the client generation: `npm ci`, then
`npm run generate:client-api`, then `npm run dev` or `npm run build`.

The JDK of `gradle/gradle-daemon-jvm.properties` must be installed: Gradle finds it whatever `java` is on the
`PATH`, but never downloads it. Integration tests need Docker. The checks of the hooks, the workflows, the
changelogs and the documentation site are in the skill of that kind of change.

## Before a change, read its skill

Each folder of `.claude/skills/` says how to carry out one kind of change. Read its `SKILL.md` before that change,
whatever the agent, even when no tool loaded it for you.

- Backend production code, such as a use case, a port, an adapter or a controller: `hexagonal-backend`
- A test: `test-driven-development` for new behavior, `test-existing-code` for code that has none
- The OpenAPI contract, never the code generated from it: `api-contract-change`. A breaking change goes through
  expand and contract, even for the frontend.
- The schema or the jOOQ classes: `database-change`
- A dependency, a version or the lock: `dependency-change`
- A convention plugin of `build-logic`: `build-convention-change`
- A workflow, the compose file or the Renovate configuration: `ci-workflow-change`
- A hook, a setting, a skill or an agent: `harness-change`
- A page under `docs/src` or a README: `documentation-change`
- The Nuxt app: `frontend-feature`. A tech starter: `new-backend-tech-starter` or `new-frontend-tech-starter`
- An issue: `new-issue`. A decision record: `new-adr`. An issue taken to a pull request: `implement-issue`

A skill names its model files by class: find one with `git grep -nwE '(class|interface|object) <Name>'`. Your
drafts, test lists and reports go under `git rev-parse --git-path claude/<file>` or in the session's scratch
directory: never in the tree, never elsewhere on the machine.

## Traps

- Jackson 3: core, databind and the format modules are `tools.jackson.*`, but the generic annotations
  (`@JsonProperty`, `@JsonTypeInfo`…) stay in `com.fasterxml.jackson.annotation`.
- The existing tech starters are wired by component scan and configured by a profile of `spring.profiles.include`.
  Copy their tests, never their wiring: a new or migrated starter follows `new-backend-tech-starter`.
- Nothing in the application creates the database schema: on a new database, apply the changelogs as
  `database-change` says before using the app.
- detekt fails the build on any finding, formatting included. Fix the code, never add to
  `code-analysis/detekt/baseline.xml`.
- The architecture tests fail the build on any broken rule. Fix the code, or tolerate the violation in the rule's
  `suppress` block with the issue that removes it, never loosen the rule:
  `docs/src/engineering/writing-code/testing/architecture-tests.md` gives each rule's fix.
- "Dependency verification failed" on a branch that touched no dependency means an upstream release landed in a
  version range: the command of `dependency-change` fixes it.

## Decisions that are the maintainer's

A structuring choice is the maintainer's: a new module or dependency, a change to the API contract, to the schema
or to a security rule. Ask before making one: the facts with their real names, one question, two to four options
of one line, your recommendation first. Once the maintainer settles it, the same pull request records it as an
ADR, with `new-adr`.

## Git and pull requests

- master only changes through a pull request with a green `CI gate`, merged by rebase, and merging is the
  maintainer's. Resolve a conflict with a local rebase and `git push --force-with-lease`: GitHub's "Resolve
  conflicts" button adds a merge commit that blocks the merge.
- A branch is named `<issue>-<short-description>`. A commit subject reads `[TOPIC] Imperative subject (#issue)`,
  reusing a topic from `git log` when one fits, and the pull request title is the first commit subject. Group
  changes by topic, not by micro-step.
- Everything written on GitHub is in English and follows "Writing issues, pull requests and comments" in
  `CONTRIBUTING.md`. An issue follows its form of `.github/ISSUE_TEMPLATE/`, as `new-issue` says.
- A pull request body follows `.github/pull_request_template.md` without its comments, through `--body-file`. It
  links its issue with `Closes #<issue>`, with `Part of #<issue>` when the issue stays open, or with
  `Follows up #<issue>` when it is already closed.

## Replies

Every answer to the maintainer in the terminal is in caveman mode, level full, from the first reply, in the
maintainer's language. A question, a security warning, an irreversible action and ordered steps are in plain
prose. `.claude/skills/caveman/SKILL.md` holds the rules and the levels. Caveman never shapes what leaves the
terminal.

## Code style

No comments by default. One or two lines at most, only for what the code cannot say: a trap, an external
constraint, why not the obvious way.

## This file

What every task needs, in under 100 lines: rules and pointers, no inventory. The detail of a kind of change lives
in its skill or its page. A change that makes one of its lines wrong fixes that line in the same pull request.
