---
name: jdk-change
description: Moves DrinkIt to another JDK version, so that the toolchain, the Gradle daemon and the CI runners change together and every tool of the build supports the new bytecode. Use it before changing the Java version, and on a Renovate pull request that bumps java-jdk.
paths:
  - "gradle/gradle-daemon-jvm.properties"
  - ".github/actions/*jdk*/**"
---

# Changing the JDK

The Java version is written in three places, and nothing derives one from another. `common-convention` turns the
catalog entry into the toolchain of every module, and the Kotlin plugin derives its `jvmTarget` from it:
`docs/src/engineering/guidelines/build-tool.md` describes the conventions. This skill gives the order, the traps,
and what proves each step.

## Where the change comes from

Renovate reads the version in one place only, the `java-version` of the CI setup action, which its Dependency
Dashboard lists as `java-jdk`. A new major waits for its checkbox there. Its pull request changes the action alone
and can still pass, since the runner image may already hold the old JDK and Gradle picks it for the daemon: complete
the branch with the steps below before it is merged. Keep the version written in the action, since it is the only
alert of a new JDK.

## 1. Check that the tools support it

Check each tool that runs on the new JDK or reads its bytecode before changing a file. The major version of a class
file is the Java version plus 44: a tool too old for the new bytecode fails on that number.

- **Gradle** runs its daemon on it: the [compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html)
  must list it under "Support for running Gradle" for the version of `gradle/wrapper/gradle-wrapper.properties`.
- **Kotlin** compiles to it twice: the modules with the `kotlin` version of `gradle/platform/pluginLibs.versions.toml`,
  and `build-logic` with the Kotlin that Gradle embeds for `kotlin-dsl`, which `./gradlew --version` prints. Both
  must accept the new [`-jvm-target`](https://kotlinlang.org/docs/compiler-reference.html#jvm-target-version).
- **CodeQL** extracts the code on CI only: its
  [supported versions](https://codeql.github.com/docs/codeql-overview/supported-languages-and-frameworks/) must
  include both the JDK and the Kotlin version.
- **GraalVM**: `api-convention` applies the native image plugin, and `nativeCompile` needs a GraalVM of the new
  version. `git grep -n nativeCompile .github` shows whether CI builds a native image.

A tool that lags moves first, in a pull request of its own: wait for Renovate, or follow the `dependency-change`
skill.

## 2. Install the JDK locally

Gradle never downloads a JDK, since the build configures no toolchain repository. Install it with SDKMAN or
IntelliJ, then check that Gradle detects it:

```
./gradlew -q javaToolchains
```

Without it, every build stops on "Unable to download toolchain matching the requirements ({languageVersion=<N>, ...".

## 3. Change the three places together

| Where | What it sets |
|---|---|
| `java` in `gradle/platform/libs.versions.toml` | The toolchain of every module and of `build-logic`: compilation, tests, `bootRun` |
| `toolchainVersion` in `gradle/gradle-daemon-jvm.properties` | The JVM of the Gradle daemon, locally and on CI |
| `java-version` in the action that runs `actions/setup-java` | The JDK installed on every CI runner that runs Gradle |

`git grep -l actions/setup-java .github` finds the action.

- Keep the daemon and the catalog on the same version. The daemon loads `build-logic`, which is compiled for the
  catalog version, and a daemon behind it fails with "Dependency requires at least JVM runtime version <N>. This
  build uses a Java <N-1> JVM."
- Edit `gradle-daemon-jvm.properties` by hand: `./gradlew updateDaemonJvm` fails with "Toolchain download
  repositories have not been configured."
- No other file names the version: a document that needs it points to `gradle/gradle-daemon-jvm.properties`. With
  `<old>` the old version, this lists only the three places before the change and nothing after it:

  ```
  git grep -nIiP '(jdk|java|jvm|temurin|toolchain)\D{0,20}(?<!\d)<old>(?!\d)' -- ':!gradle/verification-metadata.xml' ':!*.lockfile' ':!*package-lock.json'
  ```

- The frontend's client generator runs on whatever `java` is on the `PATH`, locally and on CI. It does not follow
  the toolchain and needs no change.

## 4. Check the JVM options

Each JDK removes options and restricts more APIs. Every option of `org.gradle.jvmargs` in `gradle.properties` must
still exist: a removed one fails with "Unable to start the daemon process." and "Unrecognized VM option" before the
build starts. A removed option in a `jvmArgs` of `build-logic` fails the task whose JVM it starts.

## 5. Prove it

```
./gradlew --version                    # the "Daemon JVM" line names the new version
./gradlew build detektAll --continue   # needs Docker
./gradlew :drinkit-backend:bootRun
```

- The build compiles, tests and checks every module on the new JDK, `build-logic` included.
- Read the `WARNING:` lines that the test JVMs and `bootRun` print at start. A library that uses an API the new JDK
  restricts, such as `sun.misc.Unsafe`, native access or a dynamic agent, gets bumped through `dependency-change`
  rather than silenced with a JVM flag.
- A JDK change alone changes no dependency, but a library may publish a variant per JVM version. If dependency
  verification or the lock fails, regenerate and review as the `dependency-change` skill says.
- `bootRun` starts without a stack trace.
- Reload the Gradle project in IntelliJ.

Changing the action runs every lane of CI on the new JDK. CodeQL stays out of the `CI gate`, so check that it is
green too. The three places go in one commit, under the topic of `commitMessagePrefix` in `.github/renovate.json`.

## Before you finish

- [ ] Gradle, both Kotlin compilers and CodeQL support the new version.
- [ ] The catalog's `java`, `toolchainVersion` and the action's `java-version` hold the same version, and the grep
  for the old one finds nothing.
- [ ] `./gradlew --version` names the new daemon JVM, and `./gradlew build detektAll --continue` passes.
- [ ] Every JVM option still exists, and no new `WARNING:` was silenced with a flag.
- [ ] `bootRun` starts.
- [ ] CodeQL and the `CI gate` are green.
