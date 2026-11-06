# Tooling

These are the tools the maintainer runs around Claude Code: to follow several sessions at once, spend fewer tokens,
keep the code lean and learn the tool. Two are in the repository, the `cmux-status` hook and the `caveman` skill. The
others live on the maintainer's machine, and none is needed to contribute.

| Tool | Where | What it brings |
|---|---|---|
| [cmux][cmux] | The [`cmux-status`](hooks.md) hook feeds it | A macOS terminal for several agents side by side, with a tab per session |
| Status line | Personal Claude Code setting | The branch, the model, the context window in use, the tokens spent and the rate limits, under each prompt |
| [RTK][rtk] | Personal, run by hand | Shortens the output of common commands before it reaches the agent |
| [Caveman][caveman] | The [`caveman` skill](skills.md), adapted | Terse replies to the maintainer in the terminal, every technical fact kept |
| [Ponytail][ponytail] | Personal plugin | Pushes the agent to reuse existing code and the platform before a new dependency or abstraction |
| [Gradle skills][gradle-skills] | Claude Code plugin | `gradle-best-practices` audits the build, as the [build tool](../build-and-data/build-tool.md#auditing-the-build) page explains |

## 🖥️ cmux

In [cmux][cmux], each tab takes the name of the issue a session works on and the step of the
[`/implement-issue`](skills.md) skill it is at, such as `#511 judge`. The tab also shows the verdict of the
[judge](sub-agents.md) and the state of the [CI gate](../delivery/ci-cd.md#one-required-check) of the pull request.
Outside cmux the hook does nothing.

## ✂️ RTK

`rtk init` can install a Claude Code hook that turns a shell command such as `git status` into `rtk git status`.
Claude's own read and search tools bypass that hook, so while trying RTK out, the maintainer runs it by hand.

## 🪨 Caveman

Caveman is on by default in the replies to the maintainer, as `AGENTS.md` says. It never shapes what goes to GitHub:
issues and pull requests are written in full sentences for people who were not in the session.

## 🎓 Learning

The maintainer learned the tool through the free courses of [Claude Academy][academy]. Anthropic also runs
[certifications][certifications] for the people who build with Claude.

## 📚 Sources

- [cmux][cmux], Manaflow
- [RTK][rtk], rtk-ai
- [Caveman][caveman], Julius Brussee
- [Ponytail][ponytail], Dietrich Gebert
- [Gradle skills][gradle-skills], Gradle

[cmux]: https://github.com/manaflow-ai/cmux
[rtk]: https://github.com/rtk-ai/rtk
[caveman]: https://github.com/JuliusBrussee/caveman
[ponytail]: https://github.com/DietrichGebert/ponytail
[gradle-skills]: https://github.com/gradle/gradle-skills
[academy]: https://academy.claude.com/
[certifications]: https://claude.com/blog/four-role-based-claude-certifications
