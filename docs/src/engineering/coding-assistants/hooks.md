# Hooks

A hook is a script that runs on an event, whatever the agent or the person decides. DrinkIt has two kinds, all POSIX
`sh` scripts under [`.hooks/`][hooks-folder]. Claude Code hooks run on the events of a session, before a tool call,
after it, at start or when Claude stops, and the project settings turn them on for everyone. Git hooks run on a commit
or a push, once a clone turns them on. Both guard against the agent itself as much as against a mistake: an
instruction in [`AGENTS.md`][agents-md] or a [skill](skills.md) is advisory, a hook is not. To add or change a hook,
read this page, then follow the [`harness-change` skill][skill-harness].

## ⚙️ How Claude Code runs them

[`.claude/settings.json`][settings] declares each hook with its event, a matcher on the tool and a timeout. A hook
that runs before a tool call can refuse it or ask the person. One that runs after a call, or when Claude stops, can
send its findings back, and Claude keeps working until they are fixed.

The same file holds the permissions. Merging belongs to the maintainer, and pre-push keeps master to pull requests, so
two `deny` rules refuse `gh pr merge` and `git push --no-verify`, as a second line behind the guard hook below. An
`ask` rule makes Claude ask before it edits a file under `.hooks/`, for the reason given in
[the last section](#a-hook-the-agent-cannot-rewrite). Claude Code also loads the personal settings of whoever runs
it, from `~/.claude`: they add their own hooks and rules to the project's, and a project cannot turn them off.

## 🪝 Claude Code hooks

| Hook | Runs | What it does |
|---|---|---|
| [`guard-github-protections`][guard], the guard | Before a shell command | Refuses what belongs to the maintainer: a write to a ruleset or to branch protection, a push protection bypass, a merge. Also refuses any way around the `pre-push` git hook below, such as a write to a git configuration file, which can turn the git hooks off, and asks before a command that writes under `.hooks/` |
| [`protect-generated-files`][protect] | Before a file edit | Refuses a hand edit of a file a tool writes, such as the jOOQ classes, the API client generated from the contract, the lock files and the generated documentation pages, or of a `.env` file. Its message names the command that changes the file |
| [`detekt-after-edit`][detekt] | After a Kotlin edit | Formats and checks the file with detekt, and shows the findings to Claude |
| [`compile-at-stop`][compile] | When Claude stops | When Kotlin changed, compiles it with its tests and runs the [architecture tests](../writing-code/testing/architecture-tests.md). When the frontend changed, type checks it. The errors go back to Claude, and a state already checked is not checked again, so it cannot loop |
| [`session-context`][session] | At session start | Tells Claude its branch, the issue the branch name points to, its uncommitted work, the worktrees of merged branches to clean up, and whether the git hooks run |
| [`cmux-status`][cmux] | At start, on `gh pr` commands, around the judge | In the cmux terminal, names the tab after the issue and the step of the [`/implement-issue`](skills.md) skill, and shows the verdict of the [judge](sub-agents.md) and the state of the [CI gate](../delivery/ci-cd.md#one-required-check), as [tooling](tooling.md) describes |

## 🔀 Git hooks

`git config core.hooksPath .hooks/git`, once per clone, turns them on. They guard a person working without an agent as
well.

- **[`pre-commit`][pre-commit]** refuses a staged file over 5 MB, which would stay in the history for good, and a
  `package.json` change without its lock file. When Kotlin is staged, it formats it with [`lint-kotlin`][lint-kotlin],
  a script that `detekt-after-edit` runs too, and fails when detekt does.
- **[`pre-push`][pre-push]** refuses any push, force push or deletion of master, which changes only through a pull
  request.

## 🧪 Tested like code

Claude writes most of the hooks, so each one is tested before it guards anything:

- **A bats suite per hook**, in [`.hooks/tests`][tests], feeds it the JSON Claude Code sends and checks its answer.
  The guard is tested on a table of commands, each one allowed, asked or refused.
- **Every shell a hook meets.** A hook runs under the `sh` of the machine, so the Hooks
  [lane](../delivery/ci-cd.md#lanes) of CI runs every suite under dash, `bash --posix` and macOS `sh`, after
  ShellCheck.
- **The [harness](harness.md) checks itself.** [`harness.bats`][harness-bats] fails on a hook without a suite, a
  setting that runs a missing hook or has no timeout, which could hang a session, a `paths:` glob of a skill that
  matches no file, which would leave the skill unloaded, and a relative link under `.claude/` that leads nowhere.

The commands are in the [`harness-change` skill](https://github.com/mboisnard/drinkit/blob/master/.claude/skills/harness-change/SKILL.md).

## 🔒 A hook the agent cannot rewrite

An agent that could edit a hook could switch off the check that watches it. Claude Code protects `.claude/` on its
own, but not `.hooks/`. The `ask` rule makes Claude ask before it edits a file there, and the guard asks before a
shell command that writes there. In bypassPermissions mode, where nobody answers a prompt, the guard refuses that
command instead. `harness.bats` fails when the `ask` rule disappears.

## 🧭 To try

- `onFailure: "block"` on the guard, so that a guard that crashes or times out blocks the command instead of letting
  it through.
- A prompt hook, where a model judges, for a check no script can make, such as whether a change needs an ADR. A model
  can be wrong, so it would report, never block.

## 📚 Sources

- [Automate actions with hooks](https://code.claude.com/docs/en/hooks-guide), Anthropic
- [Configure permissions](https://code.claude.com/docs/en/permissions), Anthropic
- [Bash Automated Testing System](https://github.com/bats-core/bats-core), bats-core
- [ShellCheck](https://www.shellcheck.net/), Vidar Holen

[hooks-folder]: https://github.com/mboisnard/drinkit/tree/master/.hooks
[skill-harness]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/harness-change/SKILL.md
[settings]: https://github.com/mboisnard/drinkit/blob/master/.claude/settings.json
[agents-md]: https://github.com/mboisnard/drinkit/blob/master/AGENTS.md
[guard]: https://github.com/mboisnard/drinkit/blob/master/.hooks/claude/guard-github-protections
[protect]: https://github.com/mboisnard/drinkit/blob/master/.hooks/claude/protect-generated-files
[detekt]: https://github.com/mboisnard/drinkit/blob/master/.hooks/claude/detekt-after-edit
[compile]: https://github.com/mboisnard/drinkit/blob/master/.hooks/claude/compile-at-stop
[session]: https://github.com/mboisnard/drinkit/blob/master/.hooks/claude/session-context
[cmux]: https://github.com/mboisnard/drinkit/blob/master/.hooks/claude/cmux-status
[pre-commit]: https://github.com/mboisnard/drinkit/blob/master/.hooks/git/pre-commit
[pre-push]: https://github.com/mboisnard/drinkit/blob/master/.hooks/git/pre-push
[lint-kotlin]: https://github.com/mboisnard/drinkit/blob/master/.hooks/git/lint-kotlin
[tests]: https://github.com/mboisnard/drinkit/tree/master/.hooks/tests
[harness-bats]: https://github.com/mboisnard/drinkit/blob/master/.hooks/tests/harness.bats
