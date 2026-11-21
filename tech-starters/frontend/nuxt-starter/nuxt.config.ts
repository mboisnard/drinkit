import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: ['@drinkit/i18n-starter'],
  components: {
    dirs: [],
  },
  imports: {
    autoImport: false,
  },
  // Their names carry a hash of their content
  routeRules: {
    '/_nuxt/**': { headers: { 'cache-control': 'public, max-age=31536000, immutable' } },
  },
  // The browsers of .browserslistrc, which the lint checks against
  vite: {
    build: { target: 'baseline-widely-available' },
  },
  // No enum, namespace nor parameter property: TypeScript that only strips its types
  typescript: {
    tsConfig: { compilerOptions: { erasableSyntaxOnly: true } },
    // Relative to .nuxt: the lint and test configs and the UI tests, which Nuxt leaves out of its
    // tsconfigs, run on Node
    nodeTsConfig: {
      include: ['../eslint.config.ts', '../vitest.config.ts', '../playwright.config.ts', '../test/ui/**/*.ts'],
      compilerOptions: { types: ['node'] },
    },
  },
  // The texts of the error page, under nuxt. in the catalog
  i18nCatalog: {
    prefixes: ['nuxt'],
  },
});
