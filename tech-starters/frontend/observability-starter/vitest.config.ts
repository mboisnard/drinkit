import { i18nTestSettings } from '@drinkit/i18n-starter/testing';
import { defineVitestProjects } from '@drinkit/test-starter/vitest';

// Nuxt UI as an app sets it up, rendered in the browser alone: the starter never needs a server render. Another
// layer installs its own error handler, as an alerting module does
export default defineVitestProjects({
  nuxtOverrides: {
    ...i18nTestSettings('../../../translations'),
    ssr: false,
    plugins: ['./test/nuxt/plugins/another-error-handler.ts'],
    modules: ['@nuxt/ui'],
    ui: { fonts: false, colorMode: false },
  },
});
