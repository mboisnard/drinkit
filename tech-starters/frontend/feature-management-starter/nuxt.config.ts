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
});
