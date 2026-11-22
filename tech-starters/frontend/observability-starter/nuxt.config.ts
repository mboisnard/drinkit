import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: ['@drinkit/nuxt-starter'],
  i18nCatalog: {
    prefixes: ['observability'],
  },
});
