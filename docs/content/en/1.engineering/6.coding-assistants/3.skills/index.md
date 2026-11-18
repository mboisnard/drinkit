---
title: "Skills"
description: "Where DrinkIt's skills live, how an agent comes to use one, and which skill covers each kind of change."
---

A skill is a folder whose `SKILL.md` tells an agent how to carry out one kind of task, with examples or templates
beside it when they help. `AGENTS.md` sits in the context of every session. Of a skill, Claude Code sees only the name
and description, and reads its body when a task calls for it. A procedure in a skill therefore costs no room
in the context window until it is needed. DrinkIt keeps a skill per kind of change. A skill stays advisory: what
enforces a rule is a [hook](../2.hooks.md) or a test. Before you add or change a skill, read this page, then follow the
[`harness-change` skill][skill-harness].

## 📁 Where the skills live

The skills live in [`.claude/skills`][skills-folder] for Claude Code, and Junie reads an uncommitted copy in
`.junie/skills` until a common `.agents` folder gives every agent the same skills and agents. Their format is an open
standard, Agent Skills.

## 🧭 How a skill gets used

- **By its description.** Claude loads a skill when its description matches the task, so the description says what
  the skill does and when to use it, in the words a task contains.
- **By the files touched.** The `paths:` globs of `SKILL.md` load the skill when Claude works on a matching file. A
  glob names a module by its role suffix, such as `-domain`, so that the skill loads on every application of the
  monorepo. A glob that matches nothing leaves the skill silent, so [`harness.bats`][harness-bats] fails on it.
- **By name.** `/implement-issue <issue number>` starts a skill. A skill with side effects sets
  `disable-model-invocation: true`, so that only a person starts it.
- **By [`AGENTS.md`][agents-md] and other skills.** `AGENTS.md` sends every agent to the skill of each kind of
  change, and skills chain: `backend-feature` starts by loading `test-driven-development`.

Claude may still judge that a task needs no skill. The hooks and tests catch what a skipped skill would have
prevented.

Commands, templates and long examples stay in files beside `SKILL.md`, such as `examples.md`, read only when the body
sends Claude there. `SKILL.md` links each one itself, since Claude may read only the start of a file it reaches
through another one. A skill names the classes to imitate instead of copying their code, so that it stays true when
the code changes.

## 📋 The skills of DrinkIt

| Skill | When it applies | Loaded by |
|---|---|---|
| [`implement-issue`][skill-issue] | Taking an open issue to a pull request, and cleaning up once it is merged | The maintainer, `/implement-issue <number>` |
| [`new-issue`][skill-new-issue] | Turning an idea, a bug or discovered work into an issue that follows its form | Its description, or `implement-issue` |
| [`review-pr`][skill-review-pr] | Reviewing a pull request with inline comments ranked by severity | The maintainer, `/review-pr <number>`, or an agent `implement-issue` starts |
| [`test-driven-development`][skill-tdd] | Adding or changing behavior, backend or frontend, test first | Editing Kotlin, frontend sources or a frontend tech starter, and `implement-issue` |
| [`test-existing-code`][skill-test-existing] | Covering code that has no tests, each test proven able to fail | Its description, or `test-driven-development` |
| [`backend-feature`][skill-backend] | Shaping a backend feature, from the API contract to the database | Editing production code of a `-domain`, `-infra` or `-backend` module |
| [`api-contract-change`][skill-contract] | Changing the OpenAPI contract, then the code generated from it | Editing an `-api-contract` module |
| [`database-change`][skill-database] | Changing the schema or the data, then the jOOQ classes | Editing the changelogs or the generated jOOQ code |
| [`frontend-feature`][skill-frontend] | Shaping a feature of a Nuxt app, rendered on the server with explicit imports | Editing the sources or the Nuxt configuration of a `-frontend` module, or a frontend tech starter |
| [`new-backend-tech-starter`][skill-starter] | Creating or aligning a backend [tech starter](../../2.architecture/6.tech-starters.md) | Editing a backend tech starter |
| [`new-frontend-tech-starter`][skill-frontend-starter] | Creating a frontend tech starter, a Nuxt layer and a package of the pnpm workspace | Editing a frontend tech starter |
| [`pentest`][skill-pentest] | Scanning the running application with OWASP ZAP, then triaging the findings | The maintainer, `/pentest` |
| [`threat-model`][skill-threat-model] | A STRIDE threat model of one feature or flow | The maintainer, `/threat-model` |
| [`ci-workflow-change`][skill-ci-workflow] | Changing a workflow, a [CI](../../7.delivery/1.ci-cd.md) lane, a Dockerfile, the compose file or Renovate | Editing any of them |
| [`harness-change`][skill-harness] | Changing a hook, its test, the Claude Code settings, a skill or a subagent | Editing any of them |
| [`dependency-change`][skill-dependency] | Adding or bumping a dependency, then the verification metadata and the lock | Editing `gradle/platform`, a build script or a `package.json` |
| [`build-convention-change`][skill-build-convention] | Changing a convention plugin of `build-logic` | Editing `build-logic` |
| [`documentation-change`][skill-documentation] | Writing or rewriting a page of the site or a README | Editing a page under `docs/content` or a README |
| [`new-adr`][skill-adr] | Recording a structuring choice the maintainer settled as an [ADR](../../../2.decisions/index.md) | Editing an ADR, or a settled structuring choice |
| [`caveman`][skill-caveman] | Terse replies to the maintainer in the terminal | `AGENTS.md`, in every session, or `/caveman lite`, `full` or `ultra` |

## 📚 Sources

- [Equipping agents for the real world with Agent Skills](https://www.anthropic.com/engineering/equipping-agents-for-the-real-world-with-agent-skills), Anthropic
- [Agent Skills](https://agentskills.io/), the open format
- [Extend Claude with skills](https://code.claude.com/docs/en/skills), Anthropic

[skills-folder]: https://github.com/mboisnard/drinkit/tree/master/.claude/skills
[harness-bats]: https://github.com/mboisnard/drinkit/blob/master/.hooks/tests/harness.bats
[agents-md]: https://github.com/mboisnard/drinkit/blob/master/AGENTS.md
[skill-issue]: ./implement-issue/index.md
[skill-new-issue]: ./new-issue/index.md
[skill-review-pr]: ./review-pr/index.md
[skill-tdd]: ./test-driven-development/index.md
[skill-test-existing]: ./test-existing-code/index.md
[skill-backend]: ./backend-feature/index.md
[skill-contract]: ./api-contract-change/index.md
[skill-database]: ./database-change/index.md
[skill-frontend]: ./frontend-feature/index.md
[skill-starter]: ./new-backend-tech-starter/index.md
[skill-frontend-starter]: ./new-frontend-tech-starter/index.md
[skill-pentest]: ./pentest/index.md
[skill-threat-model]: ./threat-model/index.md
[skill-ci-workflow]: ./ci-workflow-change/index.md
[skill-harness]: ./harness-change/index.md
[skill-dependency]: ./dependency-change/index.md
[skill-build-convention]: ./build-convention-change/index.md
[skill-documentation]: ./documentation-change/index.md
[skill-adr]: ./new-adr/index.md
[skill-caveman]: ./caveman/index.md
