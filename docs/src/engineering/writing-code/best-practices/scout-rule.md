# Scout rule

The scout rule asks you to leave each file you touch a little cleaner than you found it. In DrinkIt, the cleanup
goes in a commit of its own whose subject starts with `[devscout]`, in the branch of your task. A cleanup bigger
than a small improvement becomes an issue. Read this page before you clean up code on the way to another change.

## 🧭 The rule

Robert C. Martin named it the Boy Scout Rule, after the scouts' "Always leave the campground cleaner than you found
it". If you find a mess, you clean it up, whoever made it, so that the next person to open the file finds it easier
to read. Martin Fowler calls it the camp site rule, and calls the habit opportunistic refactoring.

Each change pays back a little technical debt, so the code stays easy to change without a refactoring project of
its own, and nobody dreads opening the old code. Before you commit, ask yourself:

> Have I left this file, module, or test in a better state than I found it?

The improvement stays small. Some examples quote the subject of a DrinkIt commit that made one:

- a variable or a function renamed so that its name says what it does
- dead code removed
- an old helper removed once its replacement is in place, as the id generator was in
  `[devscout] Cleanup remove old id generation util`
- a visibility narrowed, as a class of the [tech starter](../../architecture/tech-starters.md) `sse-starter` was in
  `[devscout] Make Emitter internal to restrict visibility`
- a missing annotation of the [living documentation](living-documentation.md) added, as `@Usecase` was on the
  exchange rate use cases in `[devscout] Add annotation documentation for exchange usecases`
- an obvious duplication factored out
- a missing simple test added
- a comment clarified, or replaced by a clearer name

The cleanup is never a big-bang refactor. It stops before it pulls you away from your task.

## ⚙️ How it fits DrinkIt's conventions

The [`implement-issue` skill](https://github.com/mboisnard/drinkit/blob/master/.claude/skills/implement-issue/SKILL.md)
asks that a small improvement to a file you touch goes in a commit of its own. A skill holds the instructions for
one kind of change, for people as much as for coding agents. A commit that only applies the scout rule takes
`[devscout]` as its topic, in lowercase, as the earlier scout commits of `git log` write it. Its subject follows the
format of [`AGENTS.md`](https://github.com/mboisnard/drinkit/blob/master/AGENTS.md#git-and-pull-requests), with
the issue of the branch:

```
[devscout] <imperative subject> (#<issue>)
```

The topic tells the reviewer and the reader of the history which commits are a cleanup found on the way, and which
ones do what the issue asks. The commits quoted above predate this format, which adds the issue number. A cleanup
bigger than a small improvement becomes an issue of its own, through the
[`new-issue` skill](https://github.com/mboisnard/drinkit/blob/master/.claude/skills/new-issue/SKILL.md), so that
the branch stays on its task.

## 📚 Sources

- [The Boy Scout Rule](https://github.com/97-things/97-things-every-programmer-should-know/blob/master/en/thing_08/README.md), Robert C. Martin, in 97 Things Every Programmer Should Know, O'Reilly
- [Opportunistic Refactoring](https://martinfowler.com/bliki/OpportunisticRefactoring.html), Martin Fowler
