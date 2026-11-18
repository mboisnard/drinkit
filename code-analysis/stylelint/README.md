# Stylelint config

The rule set of the CSS of every DrinkIt Nuxt app, frontend tech starter and the documentation site, in `.css` files
and in the `<style>` blocks of `.vue` files, which ESLint does not read. Published in the pnpm workspace as
`@drinkit/stylelint-config`, it formats the CSS as
[`@drinkit/eslint-config`](../eslint/README.md) formats the TypeScript: no Prettier, every finding an error.

## Usage

```json
{
  "devDependencies": {
    "@drinkit/stylelint-config": "workspace:*",
    "stylelint": "catalog:"
  }
}
```

```json
// .stylelintrc.json
{
  "extends": "@drinkit/stylelint-config"
}
```

The `lint` script of the package runs `stylelint 'app/**/*.{css,vue}' --allow-empty-input` after ESLint, and the
pre-commit hook finds the packages to lint by their `.stylelintrc.json`.

## Rules

| Rule | Setting |
|---|---|
| `@stylistic/stylelint-config` | The formatting rules that left Stylelint's core in version 16, maintained by the Stylelint Stylistic project |
| `postcss-html` on `**/*.vue` | Reads the `<style>` blocks of a Vue file |
| `@stylistic/indentation` | 2 spaces, and one level inside a `<style>` block, as inside `<template>` and `<script>` |
| `@stylistic/max-line-length` | 120 characters, as the TypeScript and the Kotlin code |
| `@stylistic/string-quotes` | Single quotes, as the TypeScript |

What the Stylistic config sets, in short:

| Rules | Setting |
|---|---|
| `block-*-brace-*` | `{` after a space, `}` on its own line in a multi-line block |
| `declaration-*` | One declaration per line, `a: b` with a space after the colon, a semicolon after the last one |
| `*-comma-*` | A space after a comma on one line, a new line after it on several |
| `*-case` | Lowercase properties, units, hex colors, at-rules and pseudo-classes |
| `number-leading-zero`, `number-no-trailing-zeros` | `0.5`, not `.5` nor `0.50` |
| `max-empty-lines`, `no-eol-whitespace`, `no-missing-end-of-source-newline` | One blank line at most, no trailing space, a final newline |

## Checks

```
pnpm --filter @drinkit/stylelint-config lint
pnpm --filter @drinkit/stylelint-config typecheck
pnpm --filter drinkit-docs lint
```

The config is typed by Stylelint's `Config`, and Node loads it as TypeScript. It has no code of its own to test: the
lint of the site, which has `<style>` blocks, proves it.
