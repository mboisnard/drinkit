import { i18nTestSettings } from '@drinkit/i18n-starter/testing';
import { defineVitestProjects } from '@drinkit/test-starter/vitest';

// The locales an app names in i18n, and the paths it names in apiClient, which the module writes in the runtime config
export default defineVitestProjects({
  nuxtOverrides: {
    ...i18nTestSettings('../../../translations'),
    runtimeConfig: { public: { apiClient: { path: '/drinkit', csrfPath: '/api/auth/csrf' } } },
  },
});
