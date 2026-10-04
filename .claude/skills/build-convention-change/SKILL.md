---
name: build-convention-change
description: Changes DrinkIt's Gradle convention plugins in build-logic, so that every module keeps building the same way and the configuration cache keeps working. Use it before adding, changing or applying a com.drinkit convention plugin.
paths:
  - "build-logic/**"
---

# Changing a build convention

Read `docs/src/engineering/guidelines/build-tool.md` first: it describes each convention and why it exists. The
"Build traps" section of `AGENTS.md` lists what fails the build. This skill gives the rules and what proves them.

## 1. Name the plugin by its file

A convention's plugin id is its file name: `com.drinkit.<name>-convention.gradle.kts` is the plugin
`com.drinkit.<name>-convention`. The file declares no `package`. Folders only group files for reading, so file
names stay unique across folders.

## 2. Keep the archetypes

A Kotlin module applies one archetype, `common-convention`, `library-convention` or `api-convention`, plus the
add-ons it needs. It configures neither Kotlin, Java nor repositories itself: what every module needs belongs in
a convention. Versions come from the catalogs of `gradle/platform`, never written in a convention.

## 3. Stay compatible with the configuration cache

The configuration cache is on for every build. A convention reads values through providers, never touches
`project` inside a task action, and registers tasks lazily. Run the build twice: the second run must print
"Configuration cache entry reused". A plugin that breaks it, as `ide-convention` did, stays out until a fixed
version exists.

## 4. Prove it

```
./gradlew build detektAll --continue
```

detekt also checks every `*.gradle.kts`, `build-logic`'s included, through the root project's `detekt` task.

## Before you finish

- [ ] The file name gives the plugin id, there is no `package`, and no other file has that name.
- [ ] No module configures Kotlin, Java or repositories, and no convention writes a version.
- [ ] The second build reuses the configuration cache.
- [ ] `build-tool.md` describes the convention when its role changed.
