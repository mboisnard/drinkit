# Build Tool - Gradle

Gradle was chosen as the **build tool for this project** because it better meets the needs of a **modular, scalable, CI/CD automation-oriented project** than alternatives like Maven.

# 🛠️ Concepts Used in the Project

## 🗂️ Automatic Detection of New Gradle Modules

The project uses **Gradle automatic multi-project includes** to:

- Automatically detects new modules placed inside `tech-starters/backend/` or `drinkit/`.
- Avoids manual maintenance of the `settings.gradle.kts` file.
- Enables **effortless scalability**, useful for **hexagonal or modular architectures**.

## 📦 Version Catalog

To **centralize and standardize dependency version management**, we use **Version Catalog** with two TOML files:

### `libs.versions.toml`

- Contains versions of dependencies used at runtime and for testing.
- Centralizes versions for `jooq`, `junit`, etc.

### `pluginLibs.versions.toml`

- Contains versions of plugins used during compilation (e.g., `kotlin`, `spring-boot`, ...).
- Allows **clean plugin version management without duplication** across `build.gradle.kts` files.

**Benefits:**

✅ Centralized and visible version management  
✅ Easy version updates in a single location  
✅ Consistent dependency alignment across all modules

## 📈 Internal Dependency Management (BOM)

We use a **Gradle Platform**, `gradle/platform`, to ensure consistent dependency versions across all modules. It imports the Spring Boot, Spring AI and Spring Cloud GCP BOMs, then pins the versions of the version catalog on top of them.

- Define an **internal BOM** for aligning dependency versions across all modules.
- Reduce version drift and prevent runtime errors due to incompatibilities.
- Expose **declarative dependency constraints** for external consumers.

## 🧩 Gradle Conventions

To maintain **consistency**, we use **modular Gradle Conventions** to factor shared configurations across modules:

The conventions live in the `build-logic` included build, grouped in folders for readability only:
a plugin id comes from the file name. Three of them are archetypes, and a Kotlin module applies one
of them plus the add-ons it needs. The contract module applies `openapi-contract-convention` alone,
and `gradle/platform` is a Java platform.

### `common-convention`

- Applied by every Kotlin module, directly or through the other two archetypes.
- Sets the Java toolchain and the Kotlin compilation, constrains every source set with the platform,
  and applies `code-analysis-convention` and `test-convention`.

### `library-convention`

- Used for **library projects** (domain, infrastructure, tech starters).
- Adds `common-convention`, `documentation-convention`, Spring context and transactions, and the
  event sourcing and kotlin starters.

### `api-convention`

- Used for **Spring Boot applications**.
- Adds `common-convention`, `documentation-convention`, `spring-boot-starter-webmvc`, the kotlin and
  monitoring starters, build and git info for Actuator, GraalVM native builds, and dependency locking.

### `test / test-fixtures-convention`

- `test-convention` runs tests on the JUnit Platform and gives every Kotlin module `test-starter`: JUnit
  Jupiter, Kotest assertions, kotlin-faker, Spring Boot test and Testcontainers support.
- `test-fixtures-convention` adds the `testFixtures` source set, where a module shares its test doubles.

### `jooq-codegen-convention`

- Generates the jOOQ classes from a running PostgreSQL into `src/generated/jooq/kotlin`, which is
  committed. Only an explicit `jooqCodegen` runs it, never the build.

### `documentation-convention`

- Runs the KSP processor of `documentation-starter`, which writes the domain and tech starter pages of
  this site. Only an explicit `kspKotlin` runs it, never the build.

### `openapi-contract-convention`

- Defines a **standard project structure** for **OpenAPI contracts**.

### `contract-first-convention`

- Configures **server code generation from OpenAPI contracts**.
- Integrates generation tools (`openapi-generator`) to automatically produce server code (controllers, models, delegates, ...) aligned with the contracts.

### `code-analysis-convention`

Applies **detekt** to every module, as the single authority on static analysis — code smells
through its own rule sets, formatting through its ktlint ruleset.

- `detektAll` runs every enabled detekt task of a project, and `check` runs them too. The same
  convention lands on the modules and on the root project, and what it enables follows from where:
  on a module, `detektMain`, `detektTest` and `detektTestFixtures`, the three that resolve types —
  the others run without a compiled classpath, so any rule needing type information silently never
  fires. On the root project, which has no source set, the plain `detekt` task pointed at every
  `*.gradle.kts` in the repository, build-logic's included, which no source set contains.
- `-Pdetekt.autoCorrect=true` fixes what can be fixed, formatting included. The property exists
  rather than detekt's own `--auto-correct` flag because that flag is a per-task Gradle option: on
  a command line naming several tasks it binds only to the one it follows, leaving the others
  silently in report-only mode. The `autoCorrect: true` entries in `detekt.yml` only declare which
  rules are *allowed* to rewrite code.
- `detektReportMergeSarif` merges the per-module SARIF reports into a single file, which the CI
  uploads to GitHub code scanning. Without the merge the upload is impossible, GitHub taking only
  a handful of SARIF files per category.
- Generated code (JOOQ under `src/generated`, OpenAPI under `build/`) is excluded.
- Any finding **fails the build** — `failOnSeverity` is tightened from detekt's default `Error`
  down to `Info`.

A single file configures it: `code-analysis/detekt/detekt.yml`, holding **only this project's
deviations** from detekt's defaults, the convention setting `buildUponDefaultConfig`. There are
four: the rules this project opts into out of the 108 detekt ships inactive. Everything else is
detekt's own default, test sources included — `**/testFixtures/**` is held to the same standard as
production code, which is what detekt does by default and costs twelve findings.

`code-analysis/detekt/baseline.xml` holds the 82 findings that predate the switch to a blocking
build. They no longer fail anything; everything new does. The file only ever shrinks — you delete a
line when you fix what it holds.

The `detektBaseline*` tasks write one file per source set rather than to that path, so regenerating
wholesale means merging their output back into it. `DetektCreateBaselineTask` does not extend
`Detekt` either, so the convention applies the generated-code exclusion to it separately: without
that it records the JOOQ and OpenAPI output too, 1117 entries instead of 82.

::: warning
detekt's ktlint ruleset reads **detekt's** configuration, not `.editorconfig`. The root
`.editorconfig` only keeps IntelliJ's own formatter aligned — the detekt IDE plugin annotates and
offers a manual auto-correct action, but does not format on save — so the values the two files
share, indentation and line length, have to be kept in agreement by hand.
:::

A `pre-commit` hook under `.githooks/` formats staged Kotlin files, enabled per clone with
`git config core.hooksPath .githooks`.

### `ide-convention`

Declares the IntelliJ settings in the build instead of committing `.idea`, which is gitignored.
IntelliJ regenerates them at every Gradle sync: the detekt and EditorConfig plugins are marked as
required, the SQL dialect is set to PostgreSQL, and Build/Run actions are delegated to Gradle.

::: warning Disabled
The convention is kept in `build-logic`, commented out and applied nowhere. On Gradle 9.8.0, `name.remal.idea-settings`
4.0.9 makes every build that stores the configuration cache exit 1 without output, so any first run of
a command, and every CI build, would fail. Applying it only during an IntelliJ sync was tried and
rejected: IntelliJ then asks for a system property that a newcomer cannot guess. It comes back once a
fixed plugin version is released. Until then, install the detekt IntelliJ plugin by hand.
:::

Once re-enabled, it belongs to the root `build.gradle.kts`, which exists for exactly this: `idea-ext` configures
`idea.project`, an extension that lives only on the root project. Gradle's own guidance calls the
root build file *"the place to configure some settings and conventions that apply globally to the
entire build, that are not configured via Settings"* — IDE settings being precisely that. What does
**not** belong there is anything touching source code: the root project has none, and module
concerns go through the conventions.

`.idea/detekt.xml` is the one versioned exception: it points the IntelliJ detekt plugin at the
project's own config, and `idea-settings` has no DSL for it.

::: tip
More Gradle best practices [here](https://docs.gradle.org/9.0.0/userguide/best_practices_general.html)
:::