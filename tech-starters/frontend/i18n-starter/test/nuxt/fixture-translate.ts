import { useTranslate } from '@drinkit/i18n-starter/translate';
import type { MessageValues, TranslationKey } from '@drinkit/i18n-starter/translate';

/**
 * The t of useTranslate for the keys of the fixture catalog. Nuxt type-checks the tests of a layer with the app that
 * extends it, whose catalog holds none of them.
 */
export function useFixtureTranslate(): (key: string, values?: MessageValues) => string {
  const { t } = useTranslate();

  // eslint-disable-next-line @typescript-eslint/no-unnecessary-type-assertion -- a string here, a key in an app
  return (key, values) => t(key as TranslationKey, values);
}
