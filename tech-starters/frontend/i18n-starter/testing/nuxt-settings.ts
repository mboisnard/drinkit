/**
 * The settings an app gives @nuxtjs/i18n and i18n-starter, English and French on the catalog folder, for the
 * `nuxtOverrides` of a layer's tests. A layer that declares its prefixes in its nuxt.config.ts needs no more.
 */
export function i18nTestSettings(catalog: string, prefixes: string[] = []): Record<string, unknown> {
  return {
    i18n: {
      locales: [
        { code: 'en', language: 'en-GB' },
        { code: 'fr', language: 'fr-FR' },
      ],
      defaultLocale: 'en',
      baseUrl: 'https://drinkit.test',
    },
    i18nCatalog: { catalog, prefixes },
  };
}
