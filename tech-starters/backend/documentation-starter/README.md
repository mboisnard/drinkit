# Documentation Starter

A [tech starter](https://github.com/mboisnard/drinkit/blob/master/docs/content/en/engineering/1.architecture/6.tech-starters.md)
that holds the annotations naming the pattern a class follows, and the KSP processor that writes the
[living documentation](https://github.com/mboisnard/drinkit/blob/master/docs/content/en/engineering/3.writing-code/1.best-practices/5.living-documentation.md)
from some of them. That page lists the annotations, explains why DrinkIt generates its pages, how to generate them
and when the build requires an annotation. The starter runs at compile time only, so it has no
auto-configuration.

```mermaid
flowchart LR
    Code["Class of the application<br/>@CoreDomain, @TechStarterTool"] --> Processor[KSP processor of the starter]
    Processor --> Pages["Pages under docs/content/en/engineering/9.resources"]
```

## Usage

No build file declares the starter: the `documentation-convention` plugin, which `library-convention`
and `api-convention` apply, adds it:

```kotlin
dependencies {
    compileOnly(project(":documentation-starter"))
    ksp(project(":documentation-starter"))
}
```

```kotlin
@CoreDomain
data class Cellar(val id: CellarId, val name: CellarName, …)

@TechStarterTool
fun interface EmailSender { … }
```

The processor writes under `docs/content/en/engineering/9.resources/`, which is gitignored:

- **`domains/`**: a page per `@CoreDomain`, and the `index.md` that lists them.
- **`tech-starters/`**: a page per tech starter, its `README.md` followed by an API Reference of its
  `@TechStarterTool` classes, functions and properties, and the `index.md` that lists them.

## Configuration

No setting.

## Tests

A test needs nothing from this starter.
