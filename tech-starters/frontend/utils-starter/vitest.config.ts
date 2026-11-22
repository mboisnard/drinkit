import { i18nTestSettings } from '@drinkit/i18n-starter/testing';
import { defineVitestProjects } from '@drinkit/test-starter/vitest';

// What an app names in i18n and i18nCatalog
export default defineVitestProjects({
  nuxtOverrides: i18nTestSettings('../../../translations'),
});
