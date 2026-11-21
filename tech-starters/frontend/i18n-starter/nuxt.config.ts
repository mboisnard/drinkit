import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  modules: ['@nuxtjs/i18n'],
  components: {
    dirs: [],
  },
  imports: {
    autoImport: false,
  },
  // A package of the workspace is source to Vite, which finds its dependencies only on the first page and then reloads
  // it. Named through the starter, since an app does not depend on intl-messageformat itself
  vite: {
    optimizeDeps: {
      include: ['@drinkit/i18n-starter > intl-messageformat'],
    },
  },
  // Relative to .nuxt: the lint and test configs, the unit tests and the helpers a package gives the tests of others,
  // which run on Node outside Nuxt's tsconfigs
  typescript: {
    nodeTsConfig: {
      include: ['../eslint.config.ts', '../vitest.config.ts', '../test/unit/**/*.ts', '../testing/**/*.ts'],
      compilerOptions: { types: ['node'] },
    },
  },
  // The URL decides the locale: a server render gives every visitor of a URL the same page
  i18n: {
    strategy: 'prefix_except_default',
    detectBrowserLanguage: false,
  },
});
