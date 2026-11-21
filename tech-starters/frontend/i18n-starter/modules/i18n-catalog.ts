import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { join, resolve } from 'node:path';
import type { LocaleObject } from '@nuxtjs/i18n';
import { addTypeTemplate, defineNuxtModule } from 'nuxt/kit';
import { messagesOf, prefixesWithoutKeys } from '../shared/catalog.ts';
import type { Messages } from '../shared/catalog.ts';
import { argumentsOf } from '../shared/message-arguments.ts';

export interface I18nCatalogOptions {
  /** The folder of the shared catalog, `en.json` and `fr.json`, relative to the app. */
  catalog?: string;
  /** The first segments of the keys a layer shows, such as `cellars` for `cellars.empty`: each layer adds its own. */
  prefixes?: string[];
}

const SOURCE_LOCALE = 'en';

/**
 * Gives @nuxtjs/i18n the messages of the prefixes the app and its layers declare, as one loader per locale, and
 * declares their keys for the type check.
 */
export default defineNuxtModule<I18nCatalogOptions>({
  meta: { name: 'i18n-catalog', configKey: 'i18nCatalog' },
  setup({ catalog, prefixes = [] }, nuxt) {
    if (!catalog) {
      return;
    }

    const folder = resolve(nuxt.options.rootDir, catalog);
    const declared = [...new Set(prefixes)];
    const locales = localesOf(nuxt.options.i18n.locales);
    const codes = locales.map(({ code }) => code);
    const source = read(folder, SOURCE_LOCALE);
    const empty = prefixesWithoutKeys(source, declared);
    if (empty.length > 0) {
      throw new Error(`i18nCatalog: no key of ${SOURCE_LOCALE}.json starts with the prefixes ${empty.join(', ')}`);
    }
    const messages = Object.fromEntries(codes.map((code) => [code, messagesOf(read(folder, code), declared)]));

    // The cache folder outlives the clean of the build folder, and @nuxtjs/i18n reads the loaders when the modules are
    // done. A loader holds its messages: the hash of the URL the browser loads them from follows the text
    const loaders = join(nuxt.options.buildDir, 'cache', 'i18n-catalog');
    mkdirSync(loaders, { recursive: true });
    for (const code of codes) {
      writeFileSync(join(loaders, `${code}.ts`), loaderOf(messages[code] ?? {}));
    }
    nuxt.hook('i18n:registerModule', (register) => {
      register({
        langDir: loaders,
        locales: locales.map((locale) => ({ ...locale, files: [{ path: `${locale.code}.ts`, cache: true }] })),
      });
    });

    nuxt.options.watch.push(folder);
    addTypeTemplate({
      filename: 'types/i18n-catalog.d.ts',
      getContents: () => keysTypeOf(messages[SOURCE_LOCALE] ?? {}),
    });
  },
});

// The locales the app names, with their language: @nuxtjs/i18n reads those of the layers' files, not the options
function localesOf(locales: Array<string | LocaleObject> | undefined = []): LocaleObject[] {
  const objects = locales.map((locale) => (typeof locale === 'string' ? { code: locale } : locale));

  return objects.filter((locale, index) => objects.findIndex(({ code }) => code === locale.code) === index);
}

function read(folder: string, code: string): Messages {
  return JSON.parse(readFileSync(join(folder, `${code}.json`), 'utf8')) as Messages;
}

function loaderOf(messages: Messages): string {
  return `import { defineI18nLocale } from '#i18n';\n\nexport default defineI18nLocale(() => (${JSON.stringify(messages, null, 2)}));\n`;
}

function keysTypeOf(messages: Messages): string {
  const members = Object.entries(messages).map(([key, message]) => `    ${JSON.stringify(key)}: ${valuesTypeOf(message)};`).join('\n');

  return `declare module '@drinkit/i18n-starter/translate' {\n  interface TranslationKeys {\n${members}\n  }\n}\n\nexport {};\n`;
}

function valuesTypeOf(message: string): string {
  const members = Object.entries(argumentsOf(message)).map(([name, type]) => `${JSON.stringify(name)}: ${type}`);

  return members.length > 0 ? `{ ${members.join('; ')} }` : 'Record<never, never>';
}
