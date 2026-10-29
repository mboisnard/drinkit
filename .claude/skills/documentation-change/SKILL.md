---
name: documentation-change
description: Writes or rewrites a page of DrinkIt's documentation site, or a README, so that a person reads it once and understands what the project does and why. Use it before adding or changing a page under docs/src or a README, and when asked to clean up, rewrite or review a page.
paths:
  - "docs/src/**/*.md"
  - "**/README.md"
---

# Writing a documentation page

The site under `docs/src` tells people how DrinkIt is built and why: the architecture, the database, the
harness, the security measures, the technologies chosen. This skill gives the order to write a page in and what
proves that it reads well. [style.md](style.md) holds the style rules, each tic with a before and an after, and
[examples.md](examples.md) the commands and a rewritten page.

## 1. Know the reader

The reader is an experienced engineer who knows the trade, Spring, SQL, REST, the common patterns, but not DrinkIt.
They read the first paragraph and the headings, then the one section they came for. Write for that peer: short,
direct, never a lesson. A well-known pattern or tool gets a sentence and a source, and the page spends its words
on what DrinkIt does with it and why. A reader who feels taught what they already know stops reading.

Agents read the pages too, but what they run belongs in `AGENTS.md` and the skills. A page explains, and links to
those files rather than copying their commands.

## 2. Gather the facts

- Read the code, configuration or class the page describes, and check each fact against it, never against the
  old page. `git grep` finds the class or the setting.
- Find the source of truth for each thing the page mentions and link it instead of repeating it: `AGENTS.md`
  or the skill of its kind of change for a command, an ADR for why a choice was made, the file that implements a
  rule, another page for a subject it already covers.
- Read [the ADR index](../../../docs/src/engineering/adr/index.md) for the decisions behind the subject.
  Where the page states a choice an ADR records, it links that ADR in one sentence, such as "ADR 0002 records why
  the changelogs are organized as below" in `database.md`: the page keeps how DrinkIt works, the ADR why. An ADR
  that does not help understand the page stays out of it.
- Before renaming a page or a heading, find what links to it: `git grep -n '<page name>' -- docs/src '*.md'`.
  The same search lists `AGENTS.md` and the skills that send agents to the page. When detail leaves the page,
  update their pointer so that it still leads to that detail.
- When the subject has an official audit or linter, run it: Squawk, or the `gradle-best-practices` skill of
  Gradle's gradle-skills plugin when it is installed. The page says that the build or the code goes through it,
  with no date, count or issue that would age. A page claims that DrinkIt follows a set of practices only when a
  check backs the claim.

## 3. Choose what the page is

A page does one job, after [Diátaxis](https://diataxis.fr/):

| Kind | Answers | In DrinkIt |
|---|---|---|
| Explanation | Why do we work this way? | `modular-monolith.md`, an ADR |
| Reference | What exactly are the rules? | The rule tables of `database.md`, the tech radars |
| How-to | How do I carry out this task? | The rebaseline steps of `database.md` |

A guideline page often does two jobs. The explanation comes first, the reference follows in sections of its own,
and steps go in a numbered list under a heading that names the task.

## 4. Shape the page

1. The title names the subject. The first paragraph says what the page covers and the one idea to keep: a
   reader who stops there knows what matters. On a page about a pattern or a practice, its first sentences present
   the pattern itself, in general terms and without DrinkIt: what it is, why it helps, what it asks of the code,
   without its history. How DrinkIt applies it comes next, in sections of its own, as in `clean-architecture.md`:
   a reader who knows the pattern recognizes it before meeting the modules. A guideline page also says when to read
   it, such as before changing a convention. An absolute there, "every" or "never", holds for the whole page: when
   a later section makes an exception, the first paragraph names it or says less.
2. On a guideline page, the next paragraph gives the fact that drives the rules, such as "a migration runs while
   users are still using the application" in `database.md`.
3. Each rule says what it protects against, so that the reader can tell when it applies. When neither the code,
   its commits nor its issue gives the reason, ask the maintainer, or keep the rule and name the missing reason
   in the pull request. Never make one up.
4. Examples come from DrinkIt and stay at the level of the page: a module, a package, a behavior, such as "the
   creation of a user triggers the verification email". Name a class only when the page is about what that class
   is, such as a use case on a page about use cases: a class is renamed, moved or split long before the idea it
   illustrates changes, and each name is one more line to fix. Never a line number. Pick the parts that last: an
   example that a later change removes takes the sentence with it.
5. A gap the reader would trip on, something not covered or not checked yet, is named where it matters. A page
   links an issue only when the issue helps understand it, such as one that lists what is left to check: the
   roadmap lives on GitHub, not in the pages.
6. Every concept that has a page on the site links to that page at its first mention in a section, with text
   that names it, even when the page is still short or empty: the link already leads to the right place, and it
   holds when the page grows. `git ls-files 'docs/src/**/*.md'` lists the pages. A file outside `docs/src` is
   linked by its GitHub URL on master, as `harness.md` does.
7. "Sources" is the last section of a page that uses outside material, an article, a book or a specification,
   after every other section. The concept comes first, then its implementation in a tool or framework: the
   pattern before the Spring Boot or Nuxt page. Each entry gives the title as link text, then the author or
   publisher. The text may name a source without linking it again.

Short is the default. The reader comes for the idea, its reasons and the shape of what DrinkIt built, then goes to
the code, a starter's README, a skill or the sources for the rest. An explanation page fits in about 100 lines of
prose, its tables and diagrams left out. Past 150, cut detail or split the page. A section earns its place when a
reader would come looking for it, and a paragraph that answers no question of the reader goes, even when it is true.

A diagram carries what a paragraph carries poorly: a flow, the exchanges between parts in their order, what talks to
what, a before and an after. Draw it next to the text it illustrates, and cut the sentences it makes redundant. One
idea per diagram, a few nodes named as the text names them, and a sentence that says what it shows. A diagram that
redraws a list adds nothing. `contract-first.md` and `ci-cd.md` hold examples.

Detail has one home, next to what it describes. The page gives the shape of the subject and the reason for each
part. The detail of one item, a setting, a class, a workaround, the version that broke something, stays in the
comments of the file that holds it or in its README, and the page links there. A walk through each setting or class
in turn buries the reasons: give the shape in a sentence and link the code. A reference list gives one line per
item, as the convention tables of `build-tool.md` do. When a rewrite comes out longer than the page it replaces,
the added words carry reasons, never more detail nor a lesson.

## 5. Write it plainly

[style.md](style.md) holds the rules of sentences, words, headings, links, lists and punctuation, and the tics of
generated text, each with what it protects the reader from. Read it before writing and again before review.

Keep the markdown portable, since the framework may change: plain markdown with frontmatter, never a
VitePress `:::` container. A callout uses the GitHub alert form, `> [!NOTE]`, `> [!TIP]`, `> [!WARNING]` or
`> [!CAUTION]`, which GitHub and VitePress both render in the same style, and a collapsible block is an HTML
`<details>`. Keep callouts for what must stand out, such as an exception the project accepts on purpose: a bold
label that names it as accepted, then its reason, in two or three sentences, as "Accepted: Spring in the domain"
in `hexagonal-architecture.md`. A diagram is a Mermaid block, which GitHub and VitePress both render and a diff
shows, never an image.

## 6. Rewrite an existing page

- Never invent a fact. Keep every true one. When one turns out wrong or stale, correct it from the code and say
  so in the pull request.
- Leave frontmatter, link targets and the code blocks that are right as they are. A code block that names a
  class the code does not have takes a real one, and a command copied from `AGENTS.md` or a skill becomes a link to it.
- A heading's anchor comes from its text. Before you change a heading, find the links to it with
  `git grep -n '<page name>.*#'`, and update them in the same change. Never add a `{#anchor}` to a heading:
  VitePress reads it, but GitHub and editor previews show it as text.
- The pull request lists what was cut and why.

## 7. Leave the generated pages alone

`./gradlew kspKotlin` writes `docs/src/engineering/resources` from the `documentation-starter` annotations and
the starters' READMEs. Change the annotation or the README instead: a Claude Code hook refuses a hand edit of a
generated page.

## Before you finish

- [ ] The tics command of [examples.md](examples.md#tics) prints nothing for the page.
- [ ] `./gradlew kspKotlin --rerun`, then `npm run docs:build` from `docs/`, passes. VitePress fails on a dead
  link.
- [ ] A fresh agent given only the page and the [fresh reader prompt](examples.md#fresh-reader) reports no DrinkIt
  term used before its definition, no rule without its reason, no passage that teaches what it already knows and
  none it could skip without losing a reason.
  Fix each finding with a link or a few words, never a new paragraph, then run it again. It always finds
  something: stop once a round reports only wording. When no agent can be started, answer the prompt yourself,
  reading as that engineer, and say so in the pull request.
- [ ] The prose of an explanation page counts about 100 lines and 150 at most, or the pull request says why it needs
  more: `awk '/^```/{c=!c;next} !c && !/^\|/ && NF' <page> | wc -l` leaves out diagrams, tables and blank lines.
- [ ] Each flow, exchange between parts or before and after that the page explains is a diagram, and the words it
  replaced are gone.
- [ ] The maintainer saw the tables and diagrams as intended, or the pull request says nobody has yet. Start
  [`npm run docs:dev`](examples.md#build-the-site) in the background for them: it never returns.
- [ ] Each concept of the page that has a page of its own links to it.
- [ ] Each choice of the page that an ADR records links to that ADR.
- [ ] A new page is in the sidebar of `docs/.vitepress/config.ts`, and the pages on nearby subjects link to it.

## Sources

- [Diátaxis](https://diataxis.fr/), Daniele Procida
- [Google developer documentation style guide](https://developers.google.com/style/highlights), Google
- [Writing for GOV.UK](https://guidance.publishing.service.gov.uk/writing-to-gov-uk-standards/writing-guidelines/clear-language/), GOV.UK
- [Signs of AI writing](https://en.wikipedia.org/wiki/Wikipedia:Signs_of_AI_writing), Wikipedia
- [Documentation principles](https://www.writethedocs.org/guide/writing/docs-principles/), Write the Docs
