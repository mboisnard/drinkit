import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: ['@drinkit/nuxt-starter'],
  // The toasts of the errors, under observability. in the catalog
  i18nCatalog: {
    prefixes: ['observability'],
  },
});
