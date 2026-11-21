import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  modules: ['@nuxtjs/i18n'],
  components: {
    dirs: [],
  },
  imports: {
    autoImport: false,
  },
  // Relative to .nuxt: the lint and test configs and the unit tests, which run on Node outside Nuxt's tsconfigs
  typescript: {
    nodeTsConfig: {
      include: ['../eslint.config.ts', '../vitest.config.ts', '../test/unit/**/*.ts'],
      compilerOptions: { types: ['node'] },
    },
  },
  // The URL decides the locale: a server render gives every visitor of a URL the same page
  i18n: {
    strategy: 'prefix_except_default',
    detectBrowserLanguage: false,
  },
});
