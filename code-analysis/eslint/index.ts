import eslintComments from '@eslint-community/eslint-plugin-eslint-comments/configs';
import { createConfigForNuxt } from '@nuxt/eslint-config';
import vitest from '@vitest/eslint-plugin';
import type { Linter } from 'eslint';
import checkFile from 'eslint-plugin-check-file';
import compat from 'eslint-plugin-compat';
import playwright from 'eslint-plugin-playwright';
import tseslint from 'typescript-eslint';
import vueParser from 'vue-eslint-parser';
import { folderImportRules } from './folder-imports.ts';
import { composableUsesReactivity } from './rules/composable-uses-reactivity.ts';
import { noAsyncComposable } from './rules/no-async-composable.ts';
import { noModuleState } from './rules/no-module-state.ts';
import { noUnguardedBrowserGlobal } from './rules/no-unguarded-browser-global.ts';

export interface DrinkitEslintOptions {
  /** The folder of the package's tsconfig.json, `import.meta.dirname` in its eslint.config.ts. */
  tsconfigRootDir: string;
}

const drinkit = {
  meta: { name: 'drinkit' },
  rules: {
    'composable-uses-reactivity': composableUsesReactivity,
    'no-async-composable': noAsyncComposable,
    'no-module-state': noModuleState,
    'no-unguarded-browser-global': noUnguardedBrowserGlobal,
  },
};

const TEST_FILES = ['**/*.test.ts', '**/*.spec.ts'];
const UI_TEST_FILES = ['**/test/ui/**/*.ts'];
const DEFAULT_EXPORT_FILES = ['**/*.vue', '**/*.config.ts', '**/router.options.ts', '**/plugins/**', '**/middleware/**',
  '**/modules/**', '**/server/**'];

export function drinkitEslint({ tsconfigRootDir }: DrinkitEslintOptions): ReturnType<typeof createConfigForNuxt> {
  return createConfigForNuxt(
    {
      features: {
        typescript: { strict: true },
        import: { package: 'eslint-plugin-import-x' },
        stylistic: {
          indent: 2,
          quotes: 'single',
          semi: true,
          commaDangle: 'always-multiline',
          arrowParens: true,
          braceStyle: '1tbs',
        },
      },
    },
    tseslint.configs.strictTypeChecked,
    tseslint.configs.stylisticTypeChecked,
    eslintComments.recommended,
    compat.configs['flat/recommended'],
    {
      linterOptions: { reportUnusedDisableDirectives: 'error' },
      languageOptions: {
        parserOptions: {
          projectService: true,
          tsconfigRootDir,
          extraFileExtensions: ['.vue'],
        },
      },
      plugins: { drinkit, 'check-file': checkFile },
      rules: {
        ...formattingRules(),
        ...typescriptRules(),
        ...vueRules(),
        ...ssrRules(),
      },
    },
    {
      // typescript-eslint turns it off for .ts files only: in a .vue file too, the type check finds an undefined name
      files: ['**/*.vue'],
      // The base config of typescript-eslint sets its parser on every file, over the Vue parser of Nuxt's config
      languageOptions: { parser: vueParser },
      rules: {
        'no-undef': 'off',
        // @stylistic/indent starts a <script> block at column 0 and would fight the indent of one level
        '@stylistic/indent': 'off',
        'vue/script-indent': ['error', 2, { baseIndent: 1, switchCase: 1 }],
        // A functional component is a function named in PascalCase
        '@typescript-eslint/naming-convention': ['error', ...namingConvention(),
          { selector: 'function', format: ['camelCase', 'PascalCase'] }],
      },
    },
    {
      // The config file of a package sits in none of its tsconfig files
      files: ['**/eslint.config.ts'],
      ...tseslint.configs.disableTypeChecked,
    },
    {
      files: DEFAULT_EXPORT_FILES,
      rules: { 'import/no-default-export': 'off' },
    },
    ...fileNameRules(),
    ...folderImportRules(),
    {
      files: [...TEST_FILES, ...UI_TEST_FILES],
      rules: {
        // Elsewhere, a test matches no Vitest project nor the Playwright folder, and never runs
        'check-file/folder-match-with-fex': ['error', { '*.{test,spec}.ts': '**/test/{unit,nuxt,ui}/**/' }],
        '@typescript-eslint/ban-ts-comment': ['error', { 'ts-expect-error': 'allow-with-description' }],
        'no-restricted-properties': ['error', { object: 'vi', property: 'mock', message: NO_MODULE_MOCK }],
        'no-restricted-imports': ['error', {
          paths: [{
            name: '@nuxt/test-utils/runtime',
            importNames: ['mockNuxtImport', 'mockComponent'],
            message: NO_MODULE_MOCK,
          }],
        }],
      },
    },
    {
      files: TEST_FILES,
      ignores: UI_TEST_FILES,
      ...vitest.configs.recommended,
    },
    {
      files: UI_TEST_FILES,
      ...playwright.configs['flat/recommended'],
    },
    {
      files: UI_TEST_FILES,
      rules: {
        // The checks of @drinkit/test-starter assert, as expect does
        'playwright/expect-expect': ['warn', {
          assertFunctionNames: ['expectAccessible', 'expectGoodWebVitals', 'expectSameServerRender'],
        }],
      },
    },
  );
}

const NO_MODULE_MOCK = 'Run the real code and replace a boundary instead of mocking a module: the HTTP call, '
  + 'or the SDK of a starter through its fake';

function formattingRules(): Linter.RulesRecord {
  return {
    '@stylistic/max-len': ['error', { code: 120, ignoreUrls: true, ignoreStrings: true, ignoreTemplateLiterals: true }],
    '@stylistic/no-multiple-empty-lines': ['error', { max: 1, maxBOF: 0, maxEOF: 0 }],
  };
}

function typescriptRules(): Linter.RulesRecord {
  return {
    'vue/no-undef-components': ['error', { ignorePatterns: ['^router-view$', '^router-link$'] }],
    'import/order': ['error', {
      groups: ['builtin', 'external', 'internal', ['parent', 'sibling', 'index']],
      pathGroups: [
        { pattern: 'vue', group: 'external', position: 'before' },
        { pattern: '{#imports,#components,#app}', group: 'external', position: 'before' },
        { pattern: '@drinkit/**', group: 'internal', position: 'before' },
        { pattern: '~/**', group: 'internal' },
      ],
      pathGroupsExcludedImportTypes: ['builtin'],
      distinctGroup: false,
      alphabetize: { order: 'asc' },
    }],
    'import/no-default-export': 'error',
    '@typescript-eslint/consistent-type-imports': 'error',
    'func-style': ['error', 'declaration'],
    '@typescript-eslint/consistent-type-definitions': ['error', 'interface'],
    '@typescript-eslint/array-type': ['error', { default: 'array-simple' }],
    '@typescript-eslint/consistent-type-assertions': ['error', {
      assertionStyle: 'as',
      objectLiteralTypeAssertions: 'never',
    }],
    '@typescript-eslint/ban-ts-comment': ['error', { 'ts-expect-error': true }],
    'no-restricted-syntax': ['error',
      { selector: 'MemberExpression[object.name="Intl"][property.name=/Format$/]', message: NO_DIRECT_FORMAT },
    ],
    'no-restricted-properties': ['error',
      { property: 'toLocaleString', message: NO_DIRECT_FORMAT },
      { property: 'toLocaleDateString', message: NO_DIRECT_FORMAT },
      { property: 'toLocaleTimeString', message: NO_DIRECT_FORMAT },
    ],
    'eqeqeq': 'error',
    '@typescript-eslint/naming-convention': ['error', ...namingConvention()],
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    '@typescript-eslint/switch-exhaustiveness-check': 'error',
    'no-console': 'error',
    '@eslint-community/eslint-comments/require-description': 'error',
  };
}

const NO_DIRECT_FORMAT = 'Format through the i18n formats, so that every number and date follows the locale';

function namingConvention(): Array<Record<string, unknown>> {
  return [
    { selector: 'default', format: ['camelCase'] },
    { selector: 'variable', format: ['camelCase', 'UPPER_CASE'] },
    { selector: 'import', format: ['camelCase', 'PascalCase'] },
    { selector: 'parameter', format: ['camelCase'], leadingUnderscore: 'allow' },
    { selector: 'typeLike', format: ['PascalCase'] },
    { selector: 'interface', format: ['PascalCase'], custom: { regex: '^I[A-Z]', match: false } },
    { selector: ['typeProperty', 'typeMethod'], format: null, modifiers: ['requiresQuotes'] },
    // The keys of an object literal often follow another format: a header, a hook, a visitor of the AST
    { selector: ['objectLiteralProperty', 'objectLiteralMethod'], format: null },
  ];
}

function vueRules(): Linter.RulesRecord {
  return {
    'vue/define-props-declaration': ['error', 'type-based'],
    'vue/define-emits-declaration': ['error', 'type-literal'],
    'vue/prefer-use-template-ref': 'error',
    'vue/no-ref-as-operand': 'error',
    'vue/no-empty-component-block': 'error',
    'vue/block-order': ['error', { order: ['template', 'script', 'style'] }],
    'vue/define-macros-order': 'error',
    'vue/no-v-html': 'error',
    'vue/require-explicit-emits': 'error',
    'vue/no-unused-properties': 'error',
  };
}

function ssrRules(): Linter.RulesRecord {
  return {
    'drinkit/no-module-state': 'error',
    'drinkit/no-unguarded-browser-global': 'error',
    'drinkit/no-async-composable': 'error',
    'drinkit/composable-uses-reactivity': 'error',
  };
}

// The Vue style guide and the Nuxt routes: the last block that matches a file sets its convention
function fileNameRules(): Linter.Config[] {
  return [
    { files: ['**/*.ts'], rules: named('KEBAB_CASE') },
    { files: ['**/composables/**/*.ts'], rules: named('use+([A-Z])*([a-zA-Z0-9])') },
    { files: ['**/*.vue'], ignores: ['**/app.vue', '**/error.vue'], rules: named('PASCAL_CASE') },
    { files: ['**/pages/**/*.vue', '**/layouts/**/*.vue'], rules: named('KEBAB_CASE') },
    { files: ['**/pages/**/[[]*.vue'], rules: { 'check-file/filename-naming-convention': 'off' } },
  ];
}

function named(convention: string): Linter.RulesRecord {
  return {
    'check-file/filename-naming-convention': ['error', { '**/*': convention }, { ignoreMiddleExtensions: true }],
  };
}
