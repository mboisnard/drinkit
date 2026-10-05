# Contributing to DrinkIt

Every change goes through an issue and a pull request, whether a person or a coding agent writes it, and a
person reads both. This guide says how to set up the project and how to write them. [AGENTS.md](AGENTS.md)
holds the commands, the structure and the code conventions, for people as much as for agents.

## Issues

"New issue" offers three forms:

- **Work item**: planned work, with a goal, its context and acceptance criteria.
- **Epic**: a theme of work whose scope items become sub-issues.
- **Bug**: something that does not work as it should.

An issue is ready to pick up when its Status in the
[DrinkIt Roadmap](https://github.com/users/mboisnard/projects/2) project is Ready, which means no
`blocked by` link is still open.

Labels:

- `epic` for a theme of work, set by the Epic form, and `bug`, set by the Bug form.
- `kind:feature` for a product capability, `kind:tech` for technical or tooling work, `kind:spike` for
  time-boxed research that ends with a decision.
- `area:*` for the part of the project, such as `area:backend`, `area:build` or `area:harness`.

## Setup

It needs Docker, the JDK of `gradle/gradle-daemon-jvm.properties`, and the Node version of `.nvmrc`. Once
per clone:

```
git config core.hooksPath .hooks/git
git config blame.ignoreRevsFile .git-blame-ignore-revs     # skip the reformatting commit in git blame
```

In IntelliJ, install the detekt plugin: `.idea/detekt.xml` points it at the project's configuration, so the
editor reports what the build reports.

`./gradlew :drinkit-backend:bootRun` starts the containers of `deployment/local/compose.yml` before the
application, and leaves them running when it stops. The database starts empty, though: nothing in the
application creates its schema. On the first run, once PostgreSQL is up, run the updater,
`UpdaterApplication` in `deployment/updater`, from IntelliJ: it applies the Liquibase changelogs and exits.
From a terminal, the `psql` command in [AGENTS.md](AGENTS.md#database-and-jooq) does the same.

## Pull requests

Branch names, commit subjects, the pull request title, how a pull request links its issue and how it gets
merged are in [AGENTS.md](AGENTS.md#git-and-pull-requests). The template prefills the body.

## Writing issues, pull requests and comments

The reader is a person who may stop after two lines. That holds for text an agent writes too.

- Put the conclusion first: the outcome of an issue, or what a pull request changes and why.
- Aim for under 400 words for an issue, its prefilled text aside, and under 300 for a pull request, code
  blocks and `<details>` aside. Link rather than repeat.
- Keep to the subject: a paragraph that changes nothing the text decides or asks goes, even when it is
  accurate.
- Give each concept one word, the one of the issue or the code, and keep it from the first line to the last.
- Describe outcomes, not a file-by-file list: the diff already shows the files.
- Quote the commands you ran with what they showed, and put long output in `<details>`.
- Write in English.

Leave out the tics of generated text: an em dash for an aside, "not X but Y" constructions, empty
transitions such as "it is worth noting", bold on random words. The text also leaves no trace of how it was
drafted: no placeholder, no restated instruction, no comment on the text itself.

## AI-assisted contributions

Coding agents are welcome, and most of this project is written with them. Whoever opens an issue or a pull
request has reviewed every line of it, the code included, and answers for it as if they had typed it. The
writing rules above apply in full: a long generated text is cut down before it is posted.

## Security and license

Report a vulnerability privately, as [SECURITY.md](SECURITY.md) explains. Contributions are licensed under
the [Apache License 2.0](LICENSE), like the rest of the project.
