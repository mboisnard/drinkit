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

### `plugins.versions.toml`

- Contains versions of plugins used during compilation (e.g., `kotlin`, `spring-boot`, ...).
- Allows **clean plugin version management without duplication** across `build.gradle.kts` files.

**Benefits:**

✅ Centralized and visible version management  
✅ Easy version updates in a single location  
✅ Consistent dependency alignment across all modules

## 📈 Internal Dependency Management (BOM)

We use a **Gradle Platform** (`platform`) combined with **Spring Dependency Management Plugin** to ensure consistent dependency versions across all modules.

- Define an **internal BOM** for aligning dependency versions across all modules.
- Reduce version drift and prevent runtime errors due to incompatibilities.
- Expose **declarative dependency constraints** for external consumers.

## 🧩 Gradle Conventions

To maintain **consistency**, we use **modular Gradle Conventions** to factor shared configurations across modules:

### `common-convention`

- Defines generic shared configurations (e.g., JVM version, Kotlin compilation, encoding, test strategy).
- **Dependency-free** and included by all other conventions.

### `library-convention`

- Used for **library projects** (domain, infrastructure, ...).
- Includes `common-no-dep-convention` + tech-starters dependencies.
- Configures the project as a **internal library**.

### `api-convention`

- Used for **Spring Boot applications**.
- Includes `common-no-dep-convention` + necessary dependencies (`spring-boot-starter-web`, tech-starters, etc.).

### `test / test-fixtures-convention`

- Configures **unit and integration test behaviors**.
- Manages test dependencies (`testcontainers`, `spring-boot-starter-test`, ...).
- Structures `testFixtures` for modules.

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
- **Adoption mode**: `ignoreFailures` is on, so detekt reports without blocking. Baselining the
  ~150 current findings would also strip them from the SARIF reports and hide them from code
  scanning, which defeats the point while the tool is being assessed.

A single file configures it: `code-analysis/detekt/detekt.yml`, holding **only this project's
deviations** from detekt's defaults, the convention setting `buildUponDefaultConfig`. There are
four: the rules this project opts into out of the 108 detekt ships inactive. Everything else is
detekt's own default, test sources included — `**/testFixtures/**` is held to the same standard as
production code, which is what detekt does by default and costs twelve findings.

There is no baseline. The old one was written in detekt 1.x's ID format, which 2.x no longer
understands, and it suppressed nothing: the analysis reported the same count with and without it.
None is needed while detekt runs in adoption mode; `detektBaselineMain` generates one on demand,
per source set, into `baseline-main.xml` rather than a single shared file.

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

It is applied by the root `build.gradle.kts`, which exists for exactly this: `idea-ext` configures
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