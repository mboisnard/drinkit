import { defineVitestProjects } from '@drinkit/test-starter/vitest';

// The path an app names in apiClient.path, which the module writes in the runtime config
export default defineVitestProjects({
  nuxtOverrides: { runtimeConfig: { public: { apiClient: { path: '/drinkit' } } } },
});
