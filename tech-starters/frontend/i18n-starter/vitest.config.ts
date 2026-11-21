import { defineVitestProjects } from '@drinkit/test-starter/vitest';
import { i18nTestSettings } from './testing/nuxt-settings.ts';

// What an app names in i18n and i18nCatalog, on a catalog and a page of the tests
export default defineVitestProjects({
  nuxtOverrides: {
    ...i18nTestSettings('./test/fixtures/translations', ['cellar']),
    dir: { pages: '../test/nuxt/pages' },
  },
});
