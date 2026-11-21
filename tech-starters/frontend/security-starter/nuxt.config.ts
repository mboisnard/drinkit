import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: ['@drinkit/api-client-starter'],
  modules: ['nuxt-security'],
  // Filled by the module from the auth options of the app
  runtimeConfig: {
    public: {
      auth: {
        currentUserPath: '',
        signInPage: '/sign-in',
        forbiddenRedirects: {},
      },
    },
  },
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
  // The texts of the pages a navigation cannot open, under auth. in the catalog
  i18nCatalog: {
    prefixes: ['auth'],
  },
  // The backend rate limits and checks its input: the module's own limiter and XSS validator would refuse twice.
  // The app is same-origin, so no other origin may read its pages
  // HSTS for a year as the backend sends it, where the module gives 180 days: OWASP ASVS 5.0 V3.4.1
  security: {
    rateLimiter: false,
    xssValidator: false,
    corsHandler: false,
    headers: {
      strictTransportSecurity: { maxAge: 31536000, includeSubdomains: true },
    },
  },
});
