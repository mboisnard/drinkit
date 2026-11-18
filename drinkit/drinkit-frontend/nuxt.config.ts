import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: ['@drinkit/nuxt-starter', '@drinkit/security-starter'],
  devtools: { enabled: true },
  compatibilityDate: '2025-07-15',
});
