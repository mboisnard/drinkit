# Subagents

A subagent is an agent that a Claude Code session starts for one task. It gets its own context window, its own
instructions, the tools it is allowed and the model it runs on, and it hands back only its report. DrinkIt uses two,
both reviewers that the [`/implement-issue`][skill-issue] skill starts before a pull request opens: a judge of the
branch against its issue, and a security reviewer. A `NEEDS_WORK` verdict sends the session back to fix its work
first. To add or change an agent, read this page, then follow the [`harness-change` skill][skill-harness].

## ⚙️ How one is made

Each agent of DrinkIt is a Markdown file in [`.claude/agents`][agents-folder]: a frontmatter with its `name`, a
`description` of when to use it, its `tools` and its `model`, then its instructions. A [skill](skills.md) or a person
names the agent to start it.

## 🎯 Why a subagent per review

- **Fresh eyes.** The judge gets the issue, the worktree, the base and head commits of the branch, the draft of the
  pull request description, and the test list, which pairs each acceptance criterion with the test or command that
  proves it. Its prompt carries no account of the work, which would anchor it on what the session that wrote the code
  believes. A summary or a hint that slips in is ignored, and named in its report.
- **Read-only.** Both agents can read, search and run commands, but have no edit tool, and their instructions forbid
  any write. A reviewer that fixed what it found would end up reviewing its own work.
- **A model for judgment.** Both run on Opus, since their whole task is judgment.

## 🧑‍⚖️ The subagents of DrinkIt

| Agent | What it checks | Started by |
|---|---|---|
| [`issue-judge`][judge] | Each acceptance criterion, rerunning the proof of the test list itself, then the design against `AGENTS.md` and the pages of this site. Returns `OK` or `NEEDS_WORK` for the head commit, with the gaps it found | `/implement-issue`, before the pull request opens |
| [`security-reviewer`][security] | Authorization of each endpoint, CSRF, CORS, headers, input validation, secrets, new dependencies, the frontend's handling of user input. Ranks its findings Blocking, Important or Minor, each with its `path:line`. It runs nothing from the branch, which on a contributor's branch would execute their code | `/implement-issue` or [`/review-pr`][skill-review-pr], when the diff touches those areas |

## 🔁 The judge in /implement-issue

- **A verdict counts for one commit.** Any commit made after it, a fix for CI included, needs a new round, since the
  judge never saw it.
- **A new judge for each round.** On `NEEDS_WORK`, the session fixes every gap and starts a new judge. Its prompt
  lists the previous gaps, each with the commit and the command output that close it: facts the judge checks first,
  not an account of the work.
- **Fifteen minutes a round.** The judge checks the acceptance criteria first and leaves the shell matrix to CI. What
  it has not checked when time runs out reads "cannot verify". Past the limit, the session asks it for its verdict,
  and `maxTurns` stops a judge that loops.
- **Two rounds, then the maintainer.** After a second `NEEDS_WORK`, the maintainer decides whether the pull request
  opens with the gaps listed.
- **The verdict in plain sight.** The pull request reads `Judge: OK, round <n>, at <commit>`, with the judge's tables
  in a folded block, so that the maintainer sees before merging which commit the judge approved.

## 🧪 To try

- `isolation: worktree` for an agent that writes code, so that it works on its own copy of the repository.
- A cheaper model, such as Haiku, for an agent that only explores.
- Skills preloaded through the `skills:` field, so that a reviewer reads the conventions it checks without the
  session passing them on.

## 📚 Sources

- [Create custom subagents](https://code.claude.com/docs/en/sub-agents), Anthropic
- [Best practices for Claude Code](https://code.claude.com/docs/en/best-practices), the cost of subagents, Anthropic

[skill-issue]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/implement-issue/SKILL.md
[skill-review-pr]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/review-pr/SKILL.md
[skill-harness]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/harness-change/SKILL.md
[agents-folder]: https://github.com/mboisnard/drinkit/tree/master/.claude/agents
[judge]: https://github.com/mboisnard/drinkit/blob/master/.claude/agents/issue-judge.md
[security]: https://github.com/mboisnard/drinkit/blob/master/.claude/agents/security-reviewer.md
