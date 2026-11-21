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
  // The module drops console calls through an esbuild option that Vite ignores since it transforms with Oxc
  security: {
    rateLimiter: false,
    xssValidator: false,
    corsHandler: false,
    removeLoggers: false,
    // The backend alone decides the size of a body, per upload usage, and answers a ProblemDetail: a second ceiling
    // here would refuse an upload the backend allows. The relay streams the body, so Nitro holds none whole
    requestSizeLimiter: false,
    headers: {
      // A fetch or a frame the policy forgets stays on the app's origin: OWASP ASVS 5.0 V3.4.3
      // No page is ever shown in a frame, as the backend answers: OWASP ASVS 5.0 V3.4.6
      contentSecurityPolicy: {
        'default-src': ['\'self\''],
        'connect-src': ['\'self\''],
        'frame-ancestors': ['\'none\''],
      },
      xFrameOptions: 'DENY',
      // For a year as the backend sends it, where the module gives 180 days: OWASP ASVS 5.0 V3.4.1
      strictTransportSecurity: { maxAge: 31536000, includeSubdomains: true },
    },
  },
});
