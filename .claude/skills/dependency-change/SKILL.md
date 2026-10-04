---
name: dependency-change
description: Adds or bumps a dependency of DrinkIt, Gradle or npm, so that the version lives in one place, the verification metadata and the lock follow, and the application really resolves the new version. Use it before adding, bumping or removing a library, a plugin or a package, and before adding a starter to the application.
paths:
  - "gradle/platform/**"
  - "**/*.gradle.kts"
  - "**/package.json"
---

# Changing a dependency

The commands are in the "Dependencies" section of `AGENTS.md`, the catalogs and the BOM in
`docs/src/engineering/guidelines/build-tool.md`. This skill gives the order and what proves each step.

A new dependency is a structuring choice: ask the maintainer first, as `AGENTS.md` says. Renovate already opens
the routine bumps.

## 1. Declare the version once

A runtime or test library goes into `gradle/platform/libs.versions.toml`, and its constraint into the BOM,
`gradle/platform/build.gradle.kts`. A build plugin goes into `gradle/platform/pluginLibs.versions.toml` alone.
Modules declare the dependency without a version, and no project declares a repository.

## 2. Regenerate the verification metadata and the lock

After adding or bumping a dependency, and after adding a starter to the application, since the starter brings
its own dependencies into `drinkit-backend`. From the root:

```
./gradlew --write-verification-metadata sha256 --refresh-dependencies --write-locks dependencies
```

Keep `--refresh-dependencies`: on a warm cache, Gradle skips metadata that a fresh CI runner downloads, and CI
fails. To move a single dependency in the lock, use `--update-locks group:name` instead of `--write-locks`.

## 3. Review both diffs

```
git diff gradle/verification-metadata.xml drinkit/drinkit-backend/gradle.lockfile
```

Expect only the artifacts of the change. A version still unchanged in `gradle.lockfile` means the application
silently kept the old one: regenerate. A verification failure on a branch that touched no dependency means an
upstream release landed in a version range, and the same command fixes it.

## 4. Build

`./gradlew build` resolves exactly what the lock lists, and fails with "Dependency verification failed" or "not
part of the dependency lock state" when step 2 was skipped.

## npm

A change to a `package.json` comes with its `package-lock.json`, from `npm install` in that folder: `pre-commit`
refuses a dependency change in a `package.json` whose lock is not staged, and CI installs with `npm ci`.

## Before you finish

- [ ] The version lives in a catalog and the BOM, and no module or project names it.
- [ ] `verification-metadata.xml` and `gradle.lockfile` were regenerated with `--refresh-dependencies`, and their
  diff holds only the expected artifacts.
- [ ] `./gradlew build` passes.
- [ ] Every changed `package.json` has its `package-lock.json` in the same commit.
