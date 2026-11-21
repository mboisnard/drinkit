import { useNuxtApp } from '#app';
import { formatDate, formatNumber } from '../shared/formats.ts';
import type { CurrencyOf, DateFormat, NumberFormat } from '../shared/formats.ts';

export type { CurrencyOf, DateFormat, NumberFormat } from '../shared/formats.ts';

/**
 * The keys of the catalog the app loads, with the values each places, which the i18n-catalog module declares from
 * its English messages.
 */
// eslint-disable-next-line @typescript-eslint/no-empty-object-type -- filled by declaration merging
export interface TranslationKeys {}

/** A key of the catalog the app loads, or any string in a starter that has no catalog of its own. */
export type TranslationKey = [keyof TranslationKeys] extends [never] ? string : Extract<keyof TranslationKeys, string>;

/** The values a message places, by the name of its argument, such as `{ count: 2 }` for `{count, plural, …}`. */
export type MessageValues = Record<string, string | number>;

/** The values t takes with a key: none for a message that places none, every one it places otherwise. */
export type ValuesOf<Key extends TranslationKey> = Key extends keyof TranslationKeys
  ? [keyof TranslationKeys[Key]] extends [never] ? [values?: TranslationKeys[Key]] : [values: TranslationKeys[Key]]
  : [values?: MessageValues];

/**
 * The messages of the catalog in the locale of the page, typed by key, and its dates and numbers in the named formats.
 * The template takes `t` from here: the `$t` of vue-i18n takes any string, and an unknown key would show as itself.
 * A date takes its time zone and an amount its currency from the caller, who knows where they come from.
 */
export function useTranslate() {
  const i18n = useNuxtApp().$i18n;

  function language(): string {
    return i18n.localeProperties.value.language ?? i18n.locale.value;
  }

  return {
    locale: i18n.locale,
    /** The language of the locale, such as `fr-FR`, which an `accept-language` header or a `lang` attribute names. */
    language,
    t: <Key extends TranslationKey>(key: Key, ...[values = {}]: ValuesOf<Key>): string => i18n.t(key, values),
    exists: (key: string): key is TranslationKey => i18n.te(key),
    d: (value: Date | number, format: DateFormat, timeZone: string): string =>
      formatDate(value, format, { language: language(), timeZone }),
    n: <Format extends NumberFormat>(value: number, format: Format, ...[currency]: CurrencyOf<Format>): string =>
      formatNumber(value, format, { language: language(), currency }),
  };
}
