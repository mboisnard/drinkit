import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: ['@drinkit/i18n-starter'],
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
    // Added to those of i18n-starter: the UI tests of the packages that render pages
    nodeTsConfig: { include: ['../playwright.config.ts', '../test/ui/**/*.ts'] },
  },
  // The texts of the error page, under nuxt. in the catalog
  i18nCatalog: {
    prefixes: ['nuxt'],
  },
});
