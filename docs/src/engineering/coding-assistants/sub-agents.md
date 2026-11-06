# Subagents

A subagent is an agent that a Claude Code session starts for one task. It gets its own context window, its own
instructions, the tools it is allowed and the model it runs on, and it hands back only its report. DrinkIt defines two,
both reviewers that the [`/implement-issue`][skill-issue] skill starts before a pull request opens: a judge of the
branch against its issue, and a security reviewer. Each is a Markdown file in [`.claude/agents`][agents-folder], a
frontmatter with its name, description, tools and model, then its instructions. Before you add or change an agent,
read this page, then follow the [`harness-change` skill][skill-harness].

## 🎯 Why a subagent per review

- **Fresh eyes.** The judge gets the issue, the commits of the branch, the draft of the pull request and the test
  list, which pairs each acceptance criterion with its proof. Its prompt carries no account of the work, which would
  anchor it on what the author believes, and it names in its report any hint that slips in.
- **Read-only.** Both agents can read, search and run commands, but have no edit tool. A reviewer that fixed what it
  found would end up reviewing its own work.
- **A model for judgment.** Both run on Opus, since their whole task is judgment.

## 🧑‍⚖️ The subagents of DrinkIt

| Agent | What it checks | Started by |
|---|---|---|
| [`issue-judge`][judge] | Each acceptance criterion, rerunning its proof, then the design against `AGENTS.md`, the skills and the [coding rules](../writing-code/best-practices/coding-rules.md). Returns a Spec and a Design verdict, and `OK` only when both are | `/implement-issue`, before the pull request opens |
| [`security-reviewer`][security] | The security-sensitive parts of the diff, from endpoints to workflows. Ranks findings Blocking, Important or Minor, and runs nothing from the branch | `/implement-issue` or [`/review-pr`][skill-review-pr], when the diff touches those areas |

## 🔁 The judge in /implement-issue

```mermaid
flowchart LR
  commit[Head commit] --> judge[New judge<br/>15 minutes]
  judge -->|NEEDS_WORK, round 1| fix[Fix every gap<br/>and commit]
  fix --> judge
  judge -->|NEEDS_WORK, round 2| maintainer[The maintainer decides]
  judge -->|OK| pr[Pull request<br/>with the verdict line]
  pr --> review[Fresh review agent<br/>findings to the maintainer]
```

A verdict counts for one commit, so any later commit, a fix for CI included, needs a new round. Each round starts a
new judge, whose prompt lists the previous gaps with the facts that close them. A round lasts 15 minutes, and what
the judge has not checked by then reads "cannot verify". The pull request reads `Judge: OK, round <n>, at <commit>`,
so the maintainer sees before merging which commit the judge approved.

Once the pull request opens, the skill starts a fresh general-purpose agent that follows `/review-pr` and hands its
findings to the maintainer without posting them. It has edit tools and runs on the model of the session: only its
prompt keeps it from changing anything.

## 📚 Sources

- [Create custom subagents](https://code.claude.com/docs/en/sub-agents), Anthropic

[skill-issue]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/implement-issue/SKILL.md
[skill-review-pr]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/review-pr/SKILL.md
[skill-harness]: https://github.com/mboisnard/drinkit/blob/master/.claude/skills/harness-change/SKILL.md
[agents-folder]: https://github.com/mboisnard/drinkit/tree/master/.claude/agents
[judge]: https://github.com/mboisnard/drinkit/blob/master/.claude/agents/issue-judge.md
[security]: https://github.com/mboisnard/drinkit/blob/master/.claude/agents/security-reviewer.md
