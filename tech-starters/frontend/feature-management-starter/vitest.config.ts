import { i18nTestSettings } from '@drinkit/i18n-starter/testing';
import { defineVitestProjects } from '@drinkit/test-starter/vitest';

// What an app names in i18n, apiClient and featureManagement
export default defineVitestProjects({
  nuxtOverrides: {
    ...i18nTestSettings('../../../translations'),
    runtimeConfig: { public: { apiClient: { path: '/drinkit', csrfPath: '' } } },
    featureManagement: { flags: ['new-checkout', 'dark-mode'], experiments: ['checkout-test'] },
  },
});
