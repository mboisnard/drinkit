import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: ['@drinkit/api-client-starter'],
  // Filled by the module from the featureManagement options of the app
  runtimeConfig: {
    public: {
      featureManagement: {
        flags: [] as string[],
        experiments: [] as string[],
      },
    },
  },
  // Relative to .nuxt: the lint and test configs, which Nuxt leaves out of its tsconfigs, run on Node
  typescript: {
    nodeTsConfig: {
      include: ['../eslint.config.ts', '../vitest.config.ts'],
      compilerOptions: { types: ['node'] },
    },
  },
});
