---
name: new-issue
description: Turns an idea, a bug or work discovered during another task into a DrinkIt issue that follows the matching issue form, with its labels, project, milestone, parent epic and blockers. Use it when asked to create an issue, or when work found on the way must be recorded rather than done.
argument-hint: <the idea or the bug, in a few words>
---

# Create an issue

An issue states an outcome and how to check it, never a step-by-step recipe. It is created as soon as its draft
is ready: the maintainer reads it on GitHub and asks for changes afterwards. The commands are in
[examples.md](examples.md).

## 1. Check it does not exist

Search the open and closed issues. When one already covers it, add a comment to that one instead.

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

Write it to `$(git rev-parse --path-format=absolute --git-path claude/issue-draft.md)`.

## 4. Pick the milestone

A milestone groups the issues of one step of the roadmap, and its progress counts only the issues it holds.

- An issue under an epic takes the epic's milestone.
- Otherwise, list the open milestones and take the one whose description covers the issue's outcome.
- When the epic has no milestone and none fits, ask the maintainer before creating the issue: propose a
  dedicated milestone with a title and a one-line description, or an existing one if it comes close. Create the
  milestone only once they agree.

## 5. Create it

Create the issue with one `kind:` label, the `area:` labels that apply, the labels the form sets itself and its
milestone, then link its parent epic and its blockers. Put it on the board in Ready when it has no open blocker,
in Backlog otherwise. When it belongs to an epic, tick or add its line in the epic's "Possible scope" checklist.

Read it back: title, labels, milestone, board status, parent and blockers are set, and it reads well without the
conversation that led to it. Then show the maintainer what was created, for the record rather than for approval:
its link, title, labels, milestone, status, parent and blockers, then its full text. Edit it when they ask.
