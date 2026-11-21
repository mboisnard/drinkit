/** The locale the catalog is written in first: the others hold its keys, and a missing message falls back to it. */
export const SOURCE_LOCALE = 'en';

/** A catalog file: flat keys, such as `cellars.empty`, to ICU messages. */
export type Messages = Record<string, string>;

/** The messages whose key starts with one of the prefixes, such as `cellars` for `cellars.empty`. */
export function messagesOf(catalog: Messages, prefixes: readonly string[]): Messages {
  return Object.fromEntries(Object.entries(catalog).filter(([key]) => prefixes.includes(prefixOf(key))));
}

/** The prefixes no key of the catalog starts with. */
export function prefixesWithoutKeys(catalog: Messages, prefixes: readonly string[]): string[] {
  const present = new Set(Object.keys(catalog).map(prefixOf));

  return prefixes.filter((prefix) => !present.has(prefix));
}

function prefixOf(key: string): string {
  return key.split('.', 1)[0] ?? key;
}
