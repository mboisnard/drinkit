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

## 📈 Gradle Platforms (to be implemented)

We plan to introduce **Gradle Platforms** to:

- Define an **internal BOM** for aligning dependency versions across all modules.
- Reduce version drift and prevent runtime errors due to incompatibilities.
- Expose **declarative dependency constraints** for external consumers.

## 🧩 Gradle Conventions

To maintain **consistency**, we use **modular Gradle Conventions** to factor shared configurations across modules:

### `common-no-dep-convention`

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

Configures tools required for **static code analysis**:

- Linters (`Detekt`, `ktlint`).
- Cyclomatic complexity checks.
- Security analysis (`dependency-check`, etc.).
- Automatically generates reports during build phases or CI pipelines.
- ...

::: tip
More Gradle best practices [here](https://docs.gradle.org/9.0.0/userguide/best_practices_general.html)
:::