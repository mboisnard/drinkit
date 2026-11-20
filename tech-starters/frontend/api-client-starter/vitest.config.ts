import { defineVitestProjects } from '@drinkit/test-starter/vitest';

// The paths an app names in apiClient, which the module writes in the runtime config
export default defineVitestProjects({
  nuxtOverrides: { runtimeConfig: { public: { apiClient: { path: '/drinkit', csrfPath: '/api/auth/csrf' } } } },
});
