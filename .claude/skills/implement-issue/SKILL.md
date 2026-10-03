---
name: implement-issue
description: Takes an open DrinkIt issue to a pull request that closes it, test first, checked by a fresh judge before the pull request is opened, and cleans up once it is merged. Run it as /implement-issue <number>.
disable-model-invocation: true
argument-hint: <issue number>
---

# Implement issue #$ARGUMENTS

The acceptance criteria are the contract, the rest of the issue is context. The maintainer merges and closes:
stop at the open pull request, never merge it, never close the issue. The command of each step is in
[examples.md](examples.md).

## 1. Pick the mode

Look for an open pull request, a worktree and a branch named `$ARGUMENTS-*`, and for the merged pull request of
each local branch found.

- An open pull request: follow-up mode, step 10, in the branch's worktree.
- A worktree or local branch whose pull request is merged: cleanup mode, step 11.
- A branch without a pull request: resume from the first step not done, in the branch's worktree.
- Nothing: step 2.

## 2. Check that the issue can be worked

Stop and tell the maintainer why only when the issue is closed. Whatever its board status, running the command
is the decision to work it: step 4 moves it to In progress. Note an open blocker for the report.

## 3. Read the issue and seed the test list

Read the whole issue, its parent epic and the code it touches. The criteria are under "Acceptance criteria", or
"Expected behavior" for a bug.

Seed the test list, `$(git rev-parse --path-format=absolute --git-path claude/test-list.md)`, from the issue's
"Verification" field when it has one. Each criterion gets a line and its proof: a test, or for documentation, CI
or configuration, a check command that fails before the change and passes after it. This one file is the proof
the judge reads.

Ask the maintainer before going on when a criterion is ambiguous or out of reach, or when the work needs a
structuring choice. Work discovered on the way becomes an issue through the `new-issue` skill. A small
improvement to a file you touch goes in a commit of its own.

## 4. Create the worktree

Create it from `origin/master` and enter it with `EnterWorktree`. If that is refused, `cd` into it and use
absolute paths. Move the issue to In progress on the board and read the status back.

## 5. Implement

Invoke the `test-driven-development` skill now: it drives the code, backend or frontend. A line proven by a check
command follows the same rhythm: run it, see it fail, change, see it pass.

## 6. Verify like CI

Run the `AGENTS.md` command of every lane the changed files hit, and keep the last lines of each output.

## 7. Draft the pull request

Write the body to `$(git rev-parse --path-format=absolute --git-path claude/pr-body.md)`.

- "Choices" is there only when the branch departs from the issue's "How to approach" section, or chooses within
  it. A bug has no such section.
- "Verification" quotes the commands with their last lines.

## 8. Run the judge

Start a fresh `issue-judge` agent and wait for its verdict. Its prompt holds the inputs of
[the example](examples.md#judge-prompt) and nothing else: no summary of your work, no hint.

When the diff touches what the `security-reviewer` agent's description lists, start it too, with the worktree and
the two commits, in every round until it returns OK, and again when a later commit touches those areas. Its
Blocking findings are gaps of the round. Its Important findings are fixed, or listed in the pull request with the
reason they wait.

On `NEEDS_WORK`, fix every gap in its whole extent, rerun its proof, commit, then start a new judge: never resume
the previous one. After a second `NEEDS_WORK`, ask the maintainer whether to open the pull request with the
remaining gaps listed, or to stop.

A verdict counts only for the head commit it names. Before any push, that commit is `git rev-parse HEAD`: a
commit made after the verdict, a fix for CI included, needs a new round.

## 9. Open the pull request

Add the verdict to "Verification": `Judge: OK, round <n>, at <short sha>` in plain sight, the judge's tables in
`<details>`. A pull request the maintainer lets open after a second `NEEDS_WORK` reads
`Judge: NEEDS_WORK, round 2, at <short sha>, opened at the maintainer's request`, with the remaining gaps.

Push, open the pull request and move the issue to In review. Wait for CI in the background, since it can outlast
a command's time limit. When it fails, read the failed log, fix, run a new round, and push once. If it fails
again, stop and report.

## 10. Follow-up mode

Read the checks, the comments and the review comments of the pull request.

- A failing check: fix it as in step 9.
- A comment: address it, or answer why not. Every comment is a change request.
- Behind master: rebase, as `AGENTS.md` says.

A code change gets a new round before it is pushed, and the verdict line of the description follows.

## 11. Clean up after the merge

Leave the worktree and work from the main checkout. Fetch master, then look at what is left of the branch.

- Uncommitted changes in the worktree, or a commit that `git cherry` marks `+`, not on master: remove nothing,
  report what is left, and stop there.
- Otherwise remove the worktree, then delete the branch with `-D`: a rebase merge rewrites the commits, so `-d`
  refuses a branch that is merged.

When no tracked file of the main checkout has changed, switch it to master and pull it, fast-forward only.
Otherwise leave it as it is and say so: a switch would carry those changes onto master.

When the issue is closed, move it to Done on the board unless it is there already, and read the status back. When
it stays open, as after a `Part of` pull request, go on to step 2: the rest of the issue is still to do.

## Report

End with the pull request link and its full description, the judge verdict, the CI state, an open blocker of the
issue, and anything left to the maintainer. In cleanup mode, end with what was removed, what was left and why,
and the commit master is on.
