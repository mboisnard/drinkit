import { i18nTestSettings } from '@drinkit/i18n-starter/testing';
import { defineVitestProjects } from '@drinkit/test-starter/vitest';

// What an app names in i18n and apiClient, and Node's EventSource, to run the contract of FakeEventSource on it
export default defineVitestProjects({
  nuxtOverrides: {
    ...i18nTestSettings('../../../translations'),
    runtimeConfig: { public: { apiClient: { path: '/drinkit', csrfPath: '' } } },
  },
  unitNodeFlags: ['--experimental-eventsource'],
});
