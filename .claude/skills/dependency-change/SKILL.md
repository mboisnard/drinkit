---
name: dependency-change
description: Adds or bumps a dependency of DrinkIt, Gradle or pnpm, so that the version lives in one place, the verification metadata and the lock follow, and the application really resolves the new version. Use it before adding, bumping or removing a library, a plugin or a package, and before adding a starter to an application.
paths:
  - "gradle/platform/**"
  - "**/*.gradle.kts"
  - "**/package.json"
  - "pnpm-workspace.yaml"
---

# Changing a dependency

The catalogs and the BOM are described in `docs/content/en/1.engineering/4.build-and-data/1.build-tool.md`. This skill gives the
order, the commands and what proves each step.

A new dependency is a structuring choice: ask the maintainer first, as `AGENTS.md` says. Renovate already opens
the routine bumps. Before a dependency between modules, read
`docs/content/en/1.engineering/2.architecture/3.hexagonal-architecture.md`.

## 1. Declare the version once

A runtime or test library goes into `gradle/platform/libs.versions.toml`, and its constraint into the BOM,
`gradle/platform/build.gradle.kts`. A tool binary that a convention downloads goes into `libs.versions.toml` alone,
since a BOM constraint does nothing for it. A build plugin goes into `gradle/platform/pluginLibs.versions.toml` alone.
Modules declare the dependency without a version, and no project declares a repository.

## 2. Regenerate the verification metadata and the lock

After adding or bumping a dependency, and after adding a starter to an application, since the starter brings
its own dependencies into the `-backend` module of the application. From the root:

```bash
./gradlew --write-verification-metadata sha256 --refresh-dependencies --write-locks dependencies
```

Keep `--refresh-dependencies`: on a warm cache, Gradle skips metadata that a fresh CI runner downloads, and CI
fails. To move a single dependency in the lock, use `--update-locks group:name` instead of `--write-locks`.

## 3. Review both diffs

```bash
git diff -- gradle/verification-metadata.xml ':(glob)**/gradle.lockfile'
```

Each module that applies `api-convention` or `job-convention` has its own `gradle.lockfile`. Expect only the
artifacts of the change. A version still unchanged in the `gradle.lockfile` of a module that uses it means that
module silently kept the old one: regenerate. A verification failure on a branch
that touched no dependency means an upstream release landed in a version range, and the same command fixes it.

## 4. Build

`./gradlew build` resolves exactly what the lock lists, and fails with "Dependency verification failed" or "not
part of the dependency lock state" when step 2 was skipped.

## pnpm

The frontend packages share one pnpm workspace, declared in `pnpm-workspace.yaml` at the root. A package's version
lives once in its `catalog`, and each `package.json` names it `catalog:`; a package of the workspace is
`workspace:*`. A dependency whose install script must run is listed `true` under `allowBuilds`, with the reason.

After a change, `pnpm install` at the root writes the one lock, `pnpm-lock.yaml`: `pre-commit` refuses a dependency
change in a `package.json` or the catalog whose lock is not staged, and CI installs with
`pnpm install --frozen-lockfile`. A release younger than `minimumReleaseAge` cannot be installed yet: wait for it,
as Renovate does.

## Before you finish

- [ ] The version lives in a catalog, and in the BOM for a library, and no module or project names it.
- [ ] `verification-metadata.xml` and each `gradle.lockfile` were regenerated with `--refresh-dependencies`, and their
  diff holds only the expected artifacts.
- [ ] `./gradlew build` passes.
- [ ] Every npm version lives in the catalog of `pnpm-workspace.yaml`, and `pnpm-lock.yaml` is in the same commit.
