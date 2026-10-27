# Tooling

These are the tools the maintainer runs around Claude Code: to follow several sessions at once, spend fewer tokens,
keep the code lean and learn the tool. Two of them are in the repository, the `cmux-status` hook and the `caveman`
skill. The others live on the maintainer's machine, and none is needed to contribute: the page says what each one
brings, for a contributor who wants to try it.

## 🖥️ cmux

[cmux][cmux] is a macOS terminal built on Ghostty for running several coding agents side by side: vertical tabs,
a notification when an agent waits for an answer, a sidebar with each workspace's branch and pull request. The
repository's [`cmux-status`](hooks.md) hook feeds it. Each tab takes the name of the issue a session works on and the
step of the [`/implement-issue`](skills.md) skill it is at, such as `#511 judge`. The tab also shows the verdict of
the [judge](sub-agents.md) and the state of the [CI gate](../delivery/ci-cd.md#one-required-check) of the pull
request. Outside cmux the hook does nothing.

## 📊 Status line

A personal Claude Code status line shows, under each prompt, the branch, the model, the share of the context window in
use, the tokens spent and the rate limits. A session that fills its context is noticed before its answers degrade.

## ✂️ RTK

[RTK][rtk] runs common commands, such as `git`, `ls` or a test runner, and shortens their output before it reaches
the agent. `rtk gain` shows the tokens saved. `rtk init` can install a Claude Code hook that turns a shell command
such as `git status` into `rtk git status`. Claude's own read and search tools bypass that hook. While trying RTK out,
the maintainer runs it by hand rather than through the hook.

## 🪨 Caveman

[Caveman][caveman] makes the agent answer tersely: every technical fact stays, only the filler goes. DrinkIt ships an
adapted copy as the [`caveman` skill](skills.md), on by default in the replies to the maintainer, who reads them in a
terminal, as `AGENTS.md` says. It never shapes what goes to GitHub: issues and pull requests are written in full
sentences for people who were not in the session, as `CONTRIBUTING.md` asks.

## 🎀 Ponytail

[Ponytail][ponytail] pushes the agent to write less code: the code that exists, the standard library and the
platform come before a new dependency or abstraction. It is a personal plugin, not part of the repository.

## 🐘 Gradle skills

Gradle publishes [agent skills of its own][gradle-skills], installed as a Claude Code plugin. Its
`gradle-best-practices` skill audits the build against the official Gradle practices, as the
[build tool](../build-and-data/build-tool.md#auditing-the-build) page explains.

## 🎓 Learning

The maintainer learned the tool through the free courses of [Claude Academy][academy], Claude Code among them, each
ending with a completion badge. Anthropic also runs [paid and proctored certifications][certifications], from
Associate to Architect, for the people who build with Claude.

## 📚 Sources

- [cmux][cmux], Manaflow
- [RTK][rtk], rtk-ai
- [Caveman][caveman], Julius Brussee
- [Ponytail][ponytail], Dietrich Gebert
- [Gradle skills][gradle-skills], Gradle
- [Customize your status line](https://code.claude.com/docs/en/statusline), Anthropic
- [Claude Academy][academy] and [Four role-based certifications][certifications], Anthropic

[cmux]: https://github.com/manaflow-ai/cmux
[rtk]: https://github.com/rtk-ai/rtk
[caveman]: https://github.com/JuliusBrussee/caveman
[ponytail]: https://github.com/DietrichGebert/ponytail
[gradle-skills]: https://github.com/gradle/gradle-skills
[academy]: https://academy.claude.com/
[certifications]: https://claude.com/blog/four-role-based-claude-certifications
