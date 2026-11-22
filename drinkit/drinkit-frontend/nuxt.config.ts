import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: [
    '@drinkit/nuxt-starter',
    '@drinkit/security-starter',
    '@drinkit/api-client-starter',
    '@drinkit/observability-starter',
    '@drinkit/feature-management-starter',
    '@drinkit/utils-starter',
  ],
  modules: ['@nuxt/ui'],
  // Every component comes from an import: Nuxt reads this setting per layer, so the app sets its own
  components: { dirs: [] },
  devtools: { enabled: true },
  css: ['~/assets/css/main.css'],
  // No font downloaded at build, and no color mode script, which carries no nonce and the CSP would refuse
  ui: { fonts: false, colorMode: false },
  compatibilityDate: '2025-07-15',
  apiClient: {
    contract: '../drinkit-api-contract/contract/api-definition.yaml',
    path: '/drinkit',
    backendUrl: 'http://127.0.0.1:8080',
    csrfPath: '/api/auth/csrf',
  },
  auth: {
    currentUserPath: '/api/user/me',
    forbiddenRedirects: { ROLE_REGISTRATION_IN_PROGRESS: '/registration' },
  },
  featureManagement: {
    flags: ['cellar-count'],
    experiments: ['cellars-heading'],
  },
  i18n: {
    locales: [
      { code: 'en', language: 'en-GB' },
      { code: 'fr', language: 'fr-FR' },
    ],
    defaultLocale: 'en',
    baseUrl: 'http://localhost:3000',
  },
  i18nCatalog: {
    catalog: '../../translations',
    prefixes: ['home', 'signIn', 'registration', 'cellars', 'layout', 'error'],
  },
  // Icons drawn as SVG: in CSS mode, one shown after hydration injects a style without a nonce, which the CSP refuses
  icon: { mode: 'svg' },
});
