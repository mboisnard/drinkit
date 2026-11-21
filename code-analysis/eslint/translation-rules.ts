import type { Linter } from 'eslint';

const UNTYPED_KEYS = 'Translate with the t of useTranslate() from @drinkit/i18n-starter/translate: '
  + 'its keys are those of the catalog, and the type check refuses one it lacks';

// For the app and the starters, not the documentation site, whose texts are its Markdown pages
export function translationRules(): Linter.Config[] {
  return [
    {
      files: ['**/*.vue'],
      // A page of a test is a fixture no visitor reads
      ignores: ['**/test/**'],
      rules: {
        'vue/no-bare-strings-in-template': 'error',
        'vue/no-restricted-syntax': ['error', { selector: 'Identifier[name=/^\\$(t|te|tm|rt|d|n)$/]', message: UNTYPED_KEYS }],
      },
    },
    {
      files: ['**/*.{ts,vue}'],
      // Not no-restricted-imports, which folder-imports.ts sets by folder, and a later block would replace
      rules: {
        '@typescript-eslint/no-restricted-imports': ['error', {
          paths: ['vue-i18n', '#i18n', '#imports'].map((name) => ({ name, importNames: ['useI18n'], message: UNTYPED_KEYS })),
        }],
      },
    },
  ];
}
