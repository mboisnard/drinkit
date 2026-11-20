import { defineVitestProjects } from '@drinkit/test-starter/vitest';

// What an app names in apiClient and auth
export default defineVitestProjects({
  nuxtOverrides: {
    // The router runs the middleware, though the starter has no page: each test adds its own
    pages: true,
    auth: {
      currentUserPath: '/api/user/me',
      forbiddenRedirects: { ROLE_REGISTRATION_IN_PROGRESS: '/registration' },
    },
    runtimeConfig: { public: { apiClient: { path: '/drinkit' } } },
  },
});
