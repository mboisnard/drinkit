import { isDateElement, isNumberElement, isPluralElement, isTimeElement, parse } from '@formatjs/icu-messageformat-parser';
import type { MessageFormatElement } from '@formatjs/icu-messageformat-parser';
import { SOURCE_LOCALE } from './catalog.ts';
import type { Messages } from './catalog.ts';
import { argumentsOf, walk } from './message-arguments.ts';

// The number styles formatjs and ICU4J both read as the same format
const PORTABLE_NUMBER_STYLES = new Set([undefined, null, 'integer', 'percent']);

/** What breaks the parity with the source locale: a missing or extra key, or a message that places other values. */
export function parityProblems(catalogs: Record<string, Messages>): string[] {
  const source = catalogs[SOURCE_LOCALE] ?? {};

  return Object.entries(catalogs)
    .filter(([locale]) => locale !== SOURCE_LOCALE)
    .flatMap(([locale, messages]) => [
      ...Object.keys(source).filter((key) => !(key in messages)).map((key) => `${locale}.json misses ${key}`),
      ...Object.keys(messages).filter((key) => !(key in source)).map((key) => `${locale}.json holds ${key}, which ${SOURCE_LOCALE}.json does not`),
      ...Object.keys(source)
        .filter((key) => key in messages && valuesOf(messages[key]) !== valuesOf(source[key]))
        .map((key) => `${locale}.json places ${valuesOf(messages[key])} in ${key}, where ${SOURCE_LOCALE}.json places ${valuesOf(source[key])}`),
    ]);
}

/** The messages that are not ICU, or that the frontend's formatjs and the backend's ICU4J would not read the same. */
export function messageProblems(messages: Messages): string[] {
  return Object.entries(messages).flatMap(([key, message]) => problemsOf(key, message));
}

function problemsOf(key: string, message: string): string[] {
  let elements: MessageFormatElement[];
  try {
    elements = parse(message, { ignoreTag: true, requiresOtherClause: true });
  } catch (error) {
    return [`${key} is not an ICU message: ${(error as Error).message}`];
  }

  return [
    ...(message.includes('\'') ? [`${key} holds an ASCII apostrophe: write ’`] : []),
    ...(/<\/?[a-z]/i.test(message) ? [`${key} holds a tag`] : []),
    ...[...new Set(walk(elements).flatMap(unportable))].map((problem) => `${key} ${problem}`),
  ];
}

function unportable(element: MessageFormatElement): string[] {
  if (isDateElement(element) || isTimeElement(element)) {
    return ['formats a date: format it with d()'];
  }
  if (isNumberElement(element) && !PORTABLE_NUMBER_STYLES.has(element.style as string | null | undefined)) {
    return ['formats a number with a skeleton or a pattern: format it with n()'];
  }
  if (isPluralElement(element) && element.offset !== 0) {
    return ['uses a plural offset'];
  }

  return [];
}

function valuesOf(message: string | undefined): string {
  try {
    return Object.keys(argumentsOf(message ?? '')).sort().join(', ');
  } catch {
    return '';
  }
}
