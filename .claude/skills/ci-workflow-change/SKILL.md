---
name: ci-workflow-change
description: Changes DrinkIt's GitHub Actions workflows, its in-repo actions and the lanes of the CI, in a way the repository accepts and that never skips a check by mistake. Use it before adding or changing a workflow, a job, a step, an action reference or a path of a lane.
paths:
  - ".github/workflows/**"
  - ".github/actions/**"
  - ".github/actionlint.yaml"
---

# Changing a workflow

The local check commands are in the "Compose file and workflows" section of `AGENTS.md`, the reasons behind each
rule in `docs/src/engineering/ci-cd.md`. This skill gives the order and what proves each step.

## 1. Shape the job

- A workflow a trigger starts, such as `ci.yml` or `weekly.yml`, sets `permissions: {}` at the top. A lane file,
  called through `workflow_call`, sets none: the job of `ci.yml` that calls it grants the ceiling. Either way,
  each job asks only for what it needs.
- Each job that runs steps has a `timeout-minutes`. A job that calls a lane file cannot: the lane's own jobs set
  theirs.
- Each checkout sets `persist-credentials: false`.
- A workflow `CI gate` waits on has no `paths` filter: a skipped workflow leaves its check pending, and a pending
  required check blocks the pull request. Lanes are skipped per job, from `ci.yml`.
- A new lane follows "Add a lane" in `ci-cd.md`: its `ci-<lane>.yml` with `on: workflow_call`, its list in
  `ci-lanes.yml`, its job in `ci.yml`, that job in the gate's `needs`, and a line on the page.

## 2. Reference actions

- An external action goes by the full SHA of its release commit, with the version as a comment:
  `uses: owner/action@<sha> # v1.2.3`. `gh api repos/<owner>/<action>/commits/<tag> --jq .sha` gives the SHA.
  The repository refuses to start a workflow that references a tag or a branch.
- An action of this repository goes by `uses: $/.github/actions/<name>`, a reusable workflow by
  `uses: $/.github/workflows/<file>.yml`, with no checkout.
- A Docker image a step runs, such as actionlint's, is pinned by its digest.

## 3. Map the paths

A new folder or file goes into the list of the lane that reads it in `.github/workflows/config/ci-lanes.yml`, or
into `none` when no job reads it. Every list reaches `known` through its anchor. A path no list knows runs every
lane. The lists hold no `!` pattern: it would match every other file.

## 4. Check locally

Run the actionlint and zizmor commands of `AGENTS.md`, from the root, with Docker. Both must print nothing to fix:
zizmor fails its job in CI rather than uploading to code scanning. actionlint reads `.github/actionlint.yaml`.

## 5. Read the run

The `CI files` job runs the same checks on the pull request. `CI gate` is the only check the master ruleset
requires, so renaming or adding a job never touches the ruleset. When a lane ran and should not have, the
`Decision` log lists the files each list matched.

## Before you finish

- [ ] Every new job has its permissions, its timeout and a checkout without credentials.
- [ ] Every external action is pinned by SHA with its version comment, every in-repo one goes by `$/`.
- [ ] Every new path is in a lane or in `none`.
- [ ] actionlint and zizmor pass locally.
- [ ] `ci-cd.md` says what changed when a lane, a job or a choice changed.
