---
name: harness-change
description: Changes DrinkIt's agent harness, its Claude Code and git hooks, its settings, its skills and its agents, so that each hook runs on every shell it meets and each skill loads where it should. Use it before adding or changing a hook, its test, the Claude Code settings, a skill or an agent.
paths:
  - ".hooks/**"
  - ".claude/settings.json"
  - ".claude/skills/**"
  - ".claude/agents/**"
---

# Changing the harness

`docs/src/engineering/harness.md` describes every part and why it exists. This skill gives the rules a hook, a
skill or an agent has to follow, and what proves them.

## Hooks

### 1. Write portable sh

- Every hook starts with `#!/bin/sh` and stays POSIX: no arrays, no `[[`, no `local`, no `sed -i` (GNU and BSD
  disagree).
- On macOS, `sh` is bash 3.2. It fails to parse a `case` written inside `$(...)`: put such a loop in a function
  and call the function from `$(...)`.
- awk is BWK awk on macOS and mawk on Ubuntu: no gawk extension.
- A hook needs `jq` to read its input. Without it, a hook exits 0, so a missing tool never blocks a session. Most
  exit silently, `guard-github-protections` warns on stderr first.

### 2. Read and answer Claude Code

- The input JSON comes on stdin. Its `cwd` follows Claude's `cd`: resolve the repository from it, and fall back to
  `$CLAUDE_PROJECT_DIR`.
- A PreToolUse hook refuses with a JSON `permissionDecision: "deny"`, and its reason names what to do instead.
- A PostToolUse or Stop hook hands findings back with `exit 2` and the findings on stderr, or a `decision` of
  `block`.
- It finishes within the timeout `.claude/settings.json` gives it. A hook that runs on every tool call or at every
  session start delays Claude each time: keep it fast, and keep network calls out of it.

### 3. Register it

Every hook lives under `.hooks/`. A Claude Code hook goes into `.hooks/claude/` and into `.claude/settings.json`
as `"$CLAUDE_PROJECT_DIR"/.hooks/claude/<name>`, with a `timeout`. A git hook goes into `.hooks/git/`, which each
clone enables with `git config core.hooksPath .hooks/git`.

### 4. Test it

Each hook has a `<hook>.test` next to it, in plain `sh`. It builds a sample repository with `mktemp -d` and
`GIT_CONFIG_GLOBAL=/dev/null`, stubs every external tool, and asserts with the helpers its suite already
defines, such as `check`, or `says` and `never_says` in `session-context.test`. A new case fails before the
change. Before pushing, run every suite:
`for t in .hooks/*/*.test; do sh "$t" || echo "FAILED $t"; done`.

### 5. Describe it

The header comment of a hook holds two lines at most, as `AGENTS.md` asks of every comment. The hook's row in
`docs/src/engineering/harness.md` says what it does, where it lives and since when.

## Skills and agents

- A skill is in English and reads well for a person. It holds rules and pointers, names its model files by class
  for `git grep -nwE '(class|interface|object) <Name>'`, and states no count or version that ages.
- Each `paths:` glob matches tracked files: `git ls-files -- ':(glob)<pattern>'` lists them. A glob that matches
  nothing leaves the skill silent, with no error.
- A skill with side effects that the maintainer starts sets `disable-model-invocation: true`.
- Working files go under `git rev-parse --git-path claude`, run from the worktree, or in the session's scratch
  directory.
- The Skills table of `docs/src/engineering/harness.md` lists every skill, and its Coding agent table every agent.

## Before you finish

- [ ] Every changed hook parses and runs under `sh` on macOS, and its test has a case that failed before.
- [ ] Every hook test suite passes.
- [ ] Every `paths:` glob matches tracked files.
- [ ] The harness page describes what changed.
