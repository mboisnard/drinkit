---
name: new-issue
description: Turns an idea, a bug or work discovered during another task into a DrinkIt issue that follows the matching issue form, with its labels, project, parent epic and blockers. Use it when asked to create an issue, or when work found on the way must be recorded rather than done.
argument-hint: <the idea or the bug, in a few words>
---

# Create an issue

An issue states an outcome and how to check it, never a step-by-step recipe. Nothing is created on GitHub
before the maintainer has seen the draft and said yes. The commands are in [examples.md](examples.md).

## 1. Check it does not exist

Search the open and closed issues. When one already covers it, propose a comment on that one instead.

## 2. Pick the form

Read the forms in `.github/ISSUE_TEMPLATE/` and pick the one whose `name` and `description` match.

## 3. Write the draft

Follow the form as `AGENTS.md` says, and the writing rules of `CONTRIBUTING.md`.

- The goal says what someone observes once it is done.
- Acceptance criteria are outcomes, each one checkable by a test or a command. "The code uses X" is not an
  outcome, "a request without a session answers 403" is.
- Verification names the test or the command that checks each criterion: whoever picks the issue up starts
  from it.
- Context says why now, with links to the issues, files or pull requests that matter.

Write it to `$(git rev-parse --path-format=absolute --git-path claude/issue-draft.md)` and show it to the
maintainer with its title, labels and, when there are some, its parent epic and blockers.

## 4. Create it

Once the maintainer says yes: create the issue with one `kind:` label, the `area:` labels that apply and the
labels the form sets itself, put it on the board in Backlog, then link its parent epic and its blockers. When it
belongs to an epic, tick or add its line in the epic's "Possible scope" checklist.

Read it back: title, labels, board status, parent and blockers are set, and it reads well without the
conversation that led to it.
