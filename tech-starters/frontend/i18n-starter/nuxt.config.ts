import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  modules: ['@nuxtjs/i18n'],
  components: {
    dirs: [],
  },
  imports: {
    autoImport: false,
  },
  // Relative to .nuxt: the lint and test configs, which Nuxt leaves out of its tsconfigs, run on Node
  typescript: {
    nodeTsConfig: {
      include: ['../eslint.config.ts', '../vitest.config.ts'],
      compilerOptions: { types: ['node'] },
    },
  },
  // The URL decides the locale: a server render gives every visitor of a URL the same page
  i18n: {
    strategy: 'prefix_except_default',
    detectBrowserLanguage: false,
  },
});
