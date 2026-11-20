import { defineNuxtConfig } from 'nuxt/config';

export default defineNuxtConfig({
  extends: ['@drinkit/nuxt-starter', '@drinkit/security-starter', '@drinkit/api-client-starter'],
  devtools: { enabled: true },
  compatibilityDate: '2025-07-15',
  apiClient: {
    contract: '../drinkit-api-contract/contract/api-definition.yaml',
    path: '/drinkit',
    backendUrl: 'http://127.0.0.1:8080',
  },
  auth: {
    currentUserPath: '/api/user/me',
    forbiddenRedirects: { ROLE_REGISTRATION_IN_PROGRESS: '/registration' },
  },
});
