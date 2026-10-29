# Skills

A skill is a folder whose `SKILL.md` tells an agent how to carry out one kind of task, with examples, templates or
scripts beside it when they help. `AGENTS.md` sits in the context of every session, while Claude Code sees only the
name and description of each skill, reads its body when a task calls for it, and opens the other files when the body
sends it there. A procedure in a skill therefore costs no room in the context window until it is needed. DrinkIt keeps
a skill per kind of change in [`.claude/skills`][skills-folder]. A skill stays advisory: what enforces a rule is a
[hook](hooks.md) or a test. To add or change a skill, read this page, then follow the
[`harness-change` skill][skill-harness].

## 🧭 How a skill gets used

- **By its description.** Claude loads a skill when its description matches the task, so the description says what
  the skill does and when to use it, in the words a task contains.
- **By the files touched.** A `paths:` field in the frontmatter of `SKILL.md` lists globs, and Claude loads the skill
  when it works on a matching file, as `database-change` does on the database changelogs. A glob names a module by its
  role suffix, such as `-domain` or `-frontend`, never by an application folder, so that the skill loads on every
  application of the monorepo. A glob that matches nothing leaves the skill silent, with no error, and
  [`harness.bats`][harness-bats] fails on such a glob.
- **By name.** `/implement-issue <issue number>` starts a skill. A skill with side effects, such as one that opens a
  pull request, sets `disable-model-invocation: true` in its frontmatter so that only a person starts it.
- **By [`AGENTS.md`][agents-md] and other skills.** `AGENTS.md` sends every agent to the skill of each kind of
  change, and skills chain: the first step of `backend-feature` is to load `test-driven-development`.

The format is an open standard, Agent Skills, which Codex and other agents read too.

## ✋ Advisory, not enforced

Claude may judge that a task does not need a skill, or skip it under pressure. DrinkIt relies on the descriptions, the
globs and `AGENTS.md` to load the skills, and on the hooks and tests to catch what a skipped skill would have
prevented. Either of these, not tried in DrinkIt yet, would make a skill hard to miss:

- a rule file in `.claude/rules/`, which Claude Code loads whenever it works on a file the rule's `paths:` matches,
  holding a single line that names the skill to read
- a hook that refuses an edit until the skill that covers the file was loaded in the session

## 📎 Examples beside the skill

The examples stay in files of their own, read only when the body sends Claude there, so they cost nothing until then:
commands and real cases in the `examples.md` of several skills, an ADR template in the `template.md` of `new-adr`, the
backend and frontend specifics in the `backend.md` and `frontend.md` of `test-driven-development`. `SKILL.md` links
each file itself, since Claude may read only the start of a file it reaches through another one. A skill names the
classes to imitate rather than copying their code, so that it stays true when the code changes. A new skill is best
tried by a fresh agent that runs it alone on a real task: the agent that wrote it would fill the gaps from what it
already knows.

## 📋 The skills of DrinkIt

| Skill | When it applies | Loaded by |
|---|---|---|
| [`implement-issue`][skill-issue] | Taking an open issue to a pull request, and cleaning up once it is merged | The maintainer, `/implement-issue <number>` |
| [`new-issue`][skill-new-issue] | Turning an idea, a bug or discovered work into an issue that follows its form | Its description, or `implement-issue` |
| [`review-pr`][skill-review-pr] | Reviewing a pull request and posting inline comments ranked by severity | The maintainer, `/review-pr <number>` |
| [`test-driven-development`][skill-tdd] | Adding or changing behavior, backend or frontend, test first | Editing Kotlin code of any module, tests included, or the sources of a `-frontend` module, and `implement-issue` |
| [`test-existing-code`][skill-test-existing] | Covering code that works but has no tests, each test proven able to fail | Its description, or `test-driven-development` |
| [`backend-feature`][skill-backend] | Shaping a feature of the Kotlin backend, from the API contract to the database, after the coding rules and the architecture pages | Editing production code of a `-domain`, `-infra` or `-backend` module |
| [`api-contract-change`][skill-contract] | Changing the OpenAPI contract, then the Kotlin interfaces and the TypeScript client generated from it | Editing an `-api-contract` module or the client generator settings of a `-frontend` module |
| [`database-change`][skill-database] | Changing the schema or the data, checked by the changelog rules, then the jOOQ classes | Editing the changelogs or the generated jOOQ code |
| [`frontend-feature`][skill-frontend] | Shaping a feature of a Nuxt app around the generated client | Editing the sources or the Nuxt configuration of a `-frontend` module |
| [`new-backend-tech-starter`][skill-starter] | Creating a backend [tech starter](../architecture/tech-starters.md), or aligning one with the conventions | Editing a backend tech starter, or its description |
| [`new-frontend-tech-starter`][skill-frontend-starter] | Creating the first frontend tech starter, a Nuxt layer | Its description |
| [`pentest`][skill-pentest] | Scanning the running application with OWASP ZAP, then triaging the findings | The maintainer, `/pentest` |
| [`threat-model`][skill-threat-model] | A STRIDE threat model of one feature or flow | The maintainer, `/threat-model` |
| [`ci-workflow-change`][skill-ci-workflow] | Changing a workflow, an action reference or a lane of the [CI](../delivery/ci-cd.md) | Editing the workflows, the in-repo actions, the local compose file or the Renovate configuration |
| [`harness-change`][skill-harness] | Changing a hook, its test, the Claude Code settings, a skill or a subagent | Editing any of them |
| [`dependency-change`][skill-dependency] | Adding or bumping a dependency, then the verification metadata and the lock | Editing `gradle/platform`, a build script or a `package.json` |
| [`build-convention-change`][skill-build-convention] | Changing a convention plugin of `build-logic` | Editing `build-logic` |
| [`documentation-change`][skill-documentation] | Writing or rewriting a page of the site or a README for a person who reads it once | Editing a page under `docs/src` or a README |
| [`new-adr`][skill-adr] | Recording a structuring choice the maintainer settled as an [ADR](../adr/index.md) | Editing an ADR, or a settled structuring choice |
| [`caveman`][skill-caveman] | Terse replies to the maintainer, who reads in a terminal and pays for every token | `AGENTS.md`, in every session, or `/caveman lite`, `full` or `ultra` |

## 🧪 To try

- Move the skills to `.agents/skills`, with `.claude/skills` as a link to that folder, so that Codex finds them
  without going through `AGENTS.md`.
- Pin a skill taken from another project by a hash of its content, so that its update is a reviewed change. Today the
  caveman skill is adapted by hand from [JuliusBrussee/caveman](https://github.com/JuliusBrussee/caveman).

## 📚 Sources

- [Equipping agents for the real world with Agent Skills](https://www.anthropic.com/engineering/equipping-agents-for-the-real-world-with-agent-skills), Anthropic
- [Agent Skills](https://agentskills.io/), the open format
- [Extend Claude with skills](https://code.claude.com/docs/en/skills), Anthropic
- [How Claude remembers your project](https://code.claude.com/docs/en/memory), path-scoped rules, Anthropic

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
