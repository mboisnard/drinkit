# Skills

A skill is a folder whose `SKILL.md` tells an agent how to carry out one kind of task, with examples or templates
beside it when they help. `AGENTS.md` sits in the context of every session. Of a skill, Claude Code sees only the name
and description, and reads its body when a task calls for it. A procedure in a skill therefore costs no room
in the context window until it is needed. DrinkIt keeps a skill per kind of change. A skill stays advisory: what
enforces a rule is a [hook](hooks.md) or a test. Before you add or change a skill, read this page, then follow the
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
| [`test-driven-development`][skill-tdd] | Adding or changing behavior, backend or frontend, test first | Editing Kotlin or frontend sources, and `implement-issue` |
| [`test-existing-code`][skill-test-existing] | Covering code that has no tests, each test proven able to fail | Its description, or `test-driven-development` |
| [`backend-feature`][skill-backend] | Shaping a backend feature, from the API contract to the database | Editing production code of a `-domain`, `-infra` or `-backend` module |
| [`api-contract-change`][skill-contract] | Changing the OpenAPI contract, then the code generated from it | Editing an `-api-contract` module or the client generator settings |
| [`database-change`][skill-database] | Changing the schema or the data, then the jOOQ classes | Editing the changelogs or the generated jOOQ code |
| [`frontend-feature`][skill-frontend] | Shaping a feature of a Nuxt app around the generated client | Editing the sources or the Nuxt configuration of a `-frontend` module |
| [`new-backend-tech-starter`][skill-starter] | Creating or aligning a backend [tech starter](../architecture/tech-starters.md) | Editing a backend tech starter |
| [`new-frontend-tech-starter`][skill-frontend-starter] | Creating the first frontend tech starter, a Nuxt layer | Its description |
| [`pentest`][skill-pentest] | Scanning the running application with OWASP ZAP, then triaging the findings | The maintainer, `/pentest` |
| [`threat-model`][skill-threat-model] | A STRIDE threat model of one feature or flow | The maintainer, `/threat-model` |
| [`ci-workflow-change`][skill-ci-workflow] | Changing a workflow, a [CI](../delivery/ci-cd.md) lane, a Dockerfile, the compose file or Renovate | Editing any of them |
| [`harness-change`][skill-harness] | Changing a hook, its test, the Claude Code settings, a skill or a subagent | Editing any of them |
| [`dependency-change`][skill-dependency] | Adding or bumping a dependency, then the verification metadata and the lock | Editing `gradle/platform`, a build script or a `package.json` |
| [`build-convention-change`][skill-build-convention] | Changing a convention plugin of `build-logic` | Editing `build-logic` |
| [`documentation-change`][skill-documentation] | Writing or rewriting a page of the site or a README | Editing a page under `docs/src` or a README |
| [`new-adr`][skill-adr] | Recording a structuring choice the maintainer settled as an [ADR](../adr/index.md) | Editing an ADR, or a settled structuring choice |
| [`caveman`][skill-caveman] | Terse replies to the maintainer in the terminal | `AGENTS.md`, in every session, or `/caveman lite`, `full` or `ultra` |

## 📚 Sources

- [Equipping agents for the real world with Agent Skills](https://www.anthropic.com/engineering/equipping-agents-for-the-real-world-with-agent-skills), Anthropic
- [Agent Skills](https://agentskills.io/), the open format
- [Extend Claude with skills](https://code.claude.com/docs/en/skills), Anthropic

[skills-folder]: https://github.com/mboisnard/drinkit/tree/master/.claude/skills
[harness-bats]: https://github.com/mboisnard/drinkit/blob/master/.hooks/tests/harness.bats
[agents-md]: https://github.com/mboisnard/drinkit/blob/master/AGENTS.md
[skill-issue]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/implement-issue/SKILL.md
[skill-new-issue]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/new-issue/SKILL.md
[skill-review-pr]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/review-pr/SKILL.md
[skill-tdd]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/test-driven-development/SKILL.md
[skill-test-existing]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/test-existing-code/SKILL.md
[skill-backend]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/backend-feature/SKILL.md
[skill-contract]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/api-contract-change/SKILL.md
[skill-database]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/database-change/SKILL.md
[skill-frontend]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/frontend-feature/SKILL.md
[skill-starter]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/new-backend-tech-starter/SKILL.md
[skill-frontend-starter]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/new-frontend-tech-starter/SKILL.md
[skill-pentest]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/pentest/SKILL.md
[skill-threat-model]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/threat-model/SKILL.md
[skill-ci-workflow]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/ci-workflow-change/SKILL.md
[skill-harness]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/harness-change/SKILL.md
[skill-dependency]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/dependency-change/SKILL.md
[skill-build-convention]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/build-convention-change/SKILL.md
[skill-documentation]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/documentation-change/SKILL.md
[skill-adr]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/new-adr/SKILL.md
[skill-caveman]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/caveman/SKILL.md
