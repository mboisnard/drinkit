import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: ['@drinkit/nuxt-starter'],
  // Relative to .nuxt: the lint and test configs, which Nuxt leaves out of its tsconfigs, run on Node
  typescript: {
    nodeTsConfig: {
      include: ['../eslint.config.ts', '../vitest.config.ts'],
      compilerOptions: { types: ['node'] },
    },
  },
  // The toasts of the errors, under observability. in the catalog
  i18nCatalog: {
    prefixes: ['observability'],
  },
});
