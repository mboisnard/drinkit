import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: ['@drinkit/nuxt-starter'],
  // Filled by the module from the apiClient options of the app
  runtimeConfig: {
    apiClient: {
      backendUrl: '',
    },
    public: {
      apiClient: {
        path: '',
      },
    },
  },
});
