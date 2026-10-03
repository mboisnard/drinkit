---
name: review-pr
description: Reviews an open DrinkIt pull request, from a contributor or from an agent, against its issue, AGENTS.md and the guidelines, then posts inline comments ranked by severity once the maintainer agrees. Run it as /review-pr <number>.
disable-model-invocation: true
argument-hint: <pull request number>
---

# Review pull request #$ARGUMENTS

The review helps the maintainer decide. It never approves, never merges, and posts nothing before the
maintainer has read the findings. The commands are in [examples.md](examples.md).

## 1. Gather

Read the pull request, its diff, its checks, and the issue it closes or is part of with its acceptance criteria.
Then check the branch out in a worktree of its own, never in the maintainer's checkout, and note its base and
head commits.

The worktree is for reading. Running the branch's code, a Gradle task, a test or an npm script, runs whatever its
build files and dependencies contain, with the maintainer's GitHub token at hand. Do it only for a branch of the
maintainer or of an agent working for them. For anyone else's branch, run nothing unless the maintainer agrees
after reading its build files, scripts and workflows.

## 2. Review

Look at what a reader of the diff alone would miss:

- **The issue.** Is each acceptance criterion met, with evidence in the description? Is anything delivered that
  the issue does not ask for and the description does not explain?
- **Correctness.** Edge cases, error paths, concurrency, a test that cannot fail.
- **Design.** The refactor checklists of the `test-driven-development` skill, and the rules of `AGENTS.md`.
- **Security.** When the diff touches what the `security-reviewer` agent's description lists, start it with the
  worktree and the two commits. Its findings join yours with their severity.
- **From outside the project.** A first-time contributor's pull request gets a closer look at workflows, scripts,
  build files and new dependencies: they run with the repository's permissions once approved.

Rank each finding:

- **Blocking**: breaks an acceptance criterion, correctness, security or a rule written in the repository.
- **Important**: should change before the merge.
- **Minor**: would make the code better, the author decides.

No praise, no restating of the diff, no taste presented as a rule.

## 3. Show, then post

Show the maintainer the findings, each with its `path:line`, severity and one or two sentences. Post only what
they keep, as a single review of inline comments, the event always `COMMENT`: approving or requesting changes is
the maintainer's call. A fix of a few lines goes in a `suggestion` block.

Remove the worktree afterwards.
