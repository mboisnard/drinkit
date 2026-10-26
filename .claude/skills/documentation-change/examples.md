# Commands and examples of documentation-change

## Tics

Prints each line, outside code blocks and inline code, that holds an em or en dash, a semicolon, an inflated word,
or an emoji anywhere but at the start of a `##` heading. Pass the pages to check:

```
perl -CSD -ne '
  if (/^\s*```/) { $code = !$code; next }
  next if $code;
  (my $prose = $_) =~ s/`[^`]*`//g;
  $prose =~ s/^## \S+ /## / if $prose =~ /^## [^\w\s]/;
  print "$ARGV:$.: $_" if $prose =~ /[\x{2013}\x{2014};\x{20E3}\x{FE0F}\x{2600}-\x{27BF}\x{1F300}-\x{1FAFF}]/
    || $prose =~ /\b(crucial|essential|seamless|robust|leverage|powerful|elegant|key benefits|in summary|worth noting|not just)\b/i;
} continue { close ARGV if eof' docs/src/engineering/<section>/<page>.md
```

A line it prints is a tic until proven otherwise, such as a semicolon inside a quoted command. The command does
not see a sentence over 25 words, "not X but Y" or a group of three: read the page for those with
[style.md](style.md).

## Build the site

From the root, then from `docs/`:

```
./gradlew kspKotlin --rerun
npm ci
npm run docs:build
npm run docs:dev
```

## Fresh reader

Start a fresh agent with this prompt and the page's path, nothing else:

```
You are an experienced backend engineer: you know Spring, Gradle, SQL, REST and the common architecture patterns,
but you have never seen DrinkIt. Read the page below once, from top to bottom, as you would on the documentation
site, and open no other file.

Then list, each item with the sentence it comes from:
1. every DrinkIt term used before it is defined, or never defined. A term linked to another page counts as
   defined, and a term of the trade you know is not a gap
2. every rule given without what it protects against
3. every sentence you had to read twice
4. what you expected the first paragraph to tell you, and it did not
5. every passage that explains what you already knew, or reads like a lesson

Report only these five lists. An empty list is a good answer.

Page: <absolute path>
```

## A rewritten page

The start of `docs/src/engineering/architecture/monorepo.md`, before:

```markdown
## 🚀 Why we choose a **Monorepo**?

We opted for a **monorepo** to centralize and simplify the management of:

- 🛠️ **Application code**: all apps (frontend & backend) live in the same repository, making it easier to navigate and understand module dependencies.
- ⚙️ **Gradle conventions**: shared build configurations ensure consistency and simplify maintenance.

## 🌟 Benefits

✅ **Immediate visibility of change impact**
- Changes in a shared library are instantly reflected in consuming projects.
- The compiler signals immediately if a consuming project is impacted.
```

After. The conclusion comes first, each reason names the DrinkIt folder it applies to, and the costs have a
section of their own:

```markdown
# Monorepo

DrinkIt keeps its applications, its tech starters, its build conventions and this documentation in one
repository. A change to a shared piece and the code that uses it land in one pull request, and the build proves
that they still fit together.

## Why one repository

- **No release between modules.** The tech starters under `tech-starters/` are projects of the same Gradle
  build, so an application uses a change to a starter at once.
- **Breaks show at compile time.** A change to a starter that breaks an application fails `./gradlew build` in
  the same pull request.
- **One set of conventions.** The convention plugins of `build-logic/` configure every module the same way.

## What it costs

A change to a starter can break an application its author never opened. CI runs the lanes that the changed
paths map to in `.github/workflows/config/ci-lanes.yml`, so the pull request shows the break. The build also
grows with the repository: Gradle's build cache and configuration cache, both on in `gradle.properties`, keep it
short.
```
