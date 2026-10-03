---
name: implement-issue
description: Takes a Ready DrinkIt issue to an open pull request that closes it, test first, checked by a fresh judge before the pull request is opened. Run it as /implement-issue <number>.
disable-model-invocation: true
argument-hint: <issue number>
---

# Implement issue #$ARGUMENTS

The acceptance criteria of the issue are the contract. Everything else in the issue is context: its scope lists
possibilities, not instructions. The maintainer reviews and merges, so never merge a pull request, close an
issue, push to master or add an entry to a baseline.

When this skill says to ask the maintainer, use `AskUserQuestion`: the fact with the real names, one question,
two to four options of one line each, your recommendation first.

## 1. Pick the mode

The argument is the bare issue number. If it came with a `#`, drop it everywhere below.

```
main=$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")
gh pr list --state open --json number,headRefName --jq '.[] | select(.headRefName | startswith("$ARGUMENTS-"))'
git -C "$main" worktree list
git -C "$main" branch --list "$ARGUMENTS-*"
```

- An open pull request exists for a `$ARGUMENTS-*` branch: follow-up mode, step 10.
- A `$ARGUMENTS-*` branch exists without a pull request: resume, from the first step not done.
- Otherwise: start at step 2.

To follow up or resume, enter the branch's worktree first, as step 4 does. If it has none, create it from the
branch: `git -C "$main" fetch origin <branch>`, then
`git -C "$main" worktree add "$main/../drinkit-worktrees/<branch>" <branch>`.

## 2. Check that the issue can be worked

```
gh issue view $ARGUMENTS --json state,title,projectItems --jq '[.state, .title, (.projectItems[] | select(.title == "DrinkIt Roadmap") | .status.name)]'
gh api repos/mboisnard/drinkit/issues/$ARGUMENTS/dependencies/blocked_by --jq '[.[] | select(.state == "open") | .number]'
git config core.hooksPath
```

Stop and tell the maintainer why if the issue is not open and Ready, if a blocker is still open, or if
`core.hooksPath` is not `.githooks`. Only the maintainer moves an issue to Ready.

## 3. Read the issue and plan the proof

Read the whole issue with `gh issue view $ARGUMENTS`, its parent epic and the code it touches. The criteria come
from "Acceptance criteria", or from "Expected behavior" for a bug.

For each criterion, decide what will prove it: a test, or for documentation, CI or configuration a check
command that fails before the change and passes after it. Write that map down: the judge receives it.

Ask the maintainer before going on when a criterion is ambiguous or out of reach, or when the work needs a
structuring choice: a new module or dependency, a change to the API contract, the schema or a security rule.
Work discovered on the way becomes a new issue that follows its form, as `AGENTS.md` describes. A small
improvement to a file you touch is fine in a commit of its own.

## 4. Create the worktree

Name the branch `$ARGUMENTS-<slug>`, the slug being three to five words of the title in kebab case.

```
main=$(dirname "$(git rev-parse --path-format=absolute --git-common-dir)")
git -C "$main" fetch origin master
git -C "$main" worktree add --no-track -b <branch> "$main/../drinkit-worktrees/<branch>" origin/master
```

Enter it with `EnterWorktree` and its path. If that is refused, `cd` into it and use absolute paths. Then move
the issue to In progress and read the status back:

```
gh project item-edit 2 --owner mboisnard --url https://github.com/mboisnard/drinkit/issues/$ARGUMENTS --field Status --value "In progress"
gh issue view $ARGUMENTS --json projectItems --jq '.projectItems[].status.name'
```

## 5. Implement

Code, backend or frontend, follows the `test-driven-development` skill: invoke it now. A criterion without a test follows the
same rhythm with its check command: run it, see it fail, change, see it pass.

Commit subjects read `[TOPIC] Imperative subject (#$ARGUMENTS)`, reusing a topic from `git log` when one fits.

## 6. Verify like CI

List the changed files with `git diff --name-only origin/master...HEAD` and match them against
`.github/workflows/config/ci-lanes.yml`. Run the `AGENTS.md` command of every lane they hit. A file no lane
lists runs them all, as in CI. Keep the last lines of each output for the pull request.

## 7. Draft the pull request

Write the body to `$(git rev-parse --path-format=absolute --git-path claude/pr-body.md)`, following
`.github/pull_request_template.md` without its comments and the writing rules of `CONTRIBUTING.md`: under 300
words outside code blocks and `<details>`.

- `Closes #$ARGUMENTS`, or `Part of #$ARGUMENTS` when the issue stays open.
- "Choices" explains each choice made against the issue's "How to approach" section.
- "Verification" quotes the commands with their last lines. Long output goes in `<details>`.

## 8. Run the judge

Start the `issue-judge` agent and wait for its verdict. Its prompt holds these inputs and nothing else, no
summary of your work and no hint:

- the issue number and the absolute path of the worktree;
- the base commit, `git merge-base origin/master HEAD`, and `HEAD`;
- the path of the pull request draft and the map from criteria to proofs;
- from the second round on, the previous gaps, each with the proof that it is closed.

On `NEEDS_WORK`, fix every gap in its whole extent, rerun its proof, commit, then start a new judge: never
resume the previous one. After a second `NEEDS_WORK`, ask the maintainer whether to open the pull request with
the remaining gaps listed, or to stop.

## 9. Open the pull request

Add the verdict to "Verification": the line `Judge: OK, round <n>, at <short sha>` in plain sight, the judge's
tables in `<details>`. A pull request the maintainer lets open after a second `NEEDS_WORK` reads
`Judge: NEEDS_WORK, round 2, at <short sha>, opened at the maintainer's request`, with the remaining gaps
listed. Then:

```
git push -u origin HEAD
gh pr create --base master --title "$(git log --reverse --format=%s origin/master..HEAD | head -n 1)" --body-file <draft>
gh project item-edit 2 --owner mboisnard --url https://github.com/mboisnard/drinkit/issues/$ARGUMENTS --field Status --value "In review"
```

Wait for CI with `gh pr checks <number> --watch`, run in the background since it can outlast a command's time
limit. When it fails, read `gh run view <run> --log-failed`, fix, run a new judge if code changed, and push
once. If it fails again, stop and report.

## 10. Follow-up mode

The pull request is open. Look at what it needs:

```
gh pr checks <number>
gh pr view <number> --comments
gh api repos/mboisnard/drinkit/pulls/<number>/comments
```

- A failing check: fix it as in step 9.
- A review comment: address it, or answer why not. Every comment is a change request.
- Behind master: `git fetch origin master && git rebase origin/master`, then `git push --force-with-lease`.

A code change gets a new judge before it is pushed, and the verdict line of the description is updated.

## Report

End with the pull request link, the judge verdict, the CI state, and anything left to the maintainer.
