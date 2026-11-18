import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  modules: ['nuxt-security'],
  // A page may hold the data of a signed-in user, and its nonce changes on each response
  routeRules: {
    '/**': { headers: { 'cache-control': 'no-store' } },
  },
  // Relative to .nuxt: the lint config, which Nuxt leaves out of its tsconfigs, runs on Node
  typescript: {
    nodeTsConfig: {
      include: ['../eslint.config.ts'],
      compilerOptions: { types: ['node'] },
    },
  },
  // The backend rate limits and checks its input: the module's own limiter and XSS validator would refuse twice.
  // The app is same-origin, so no other origin may read its pages
  security: {
    rateLimiter: false,
    xssValidator: false,
    corsHandler: false,
  },
});
