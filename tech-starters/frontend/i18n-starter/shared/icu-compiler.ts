import { IntlMessageFormat } from 'intl-messageformat';

/** What vue-i18n calls to render a message, with the values of t. */
export type CompiledMessage = (context: { values: unknown }) => string;

// vue-i18n compiles a message at each call of t: the formats are kept, one per locale and message
const formats = new Map<string, IntlMessageFormat>();

/**
 * Compiles the ICU messages of the catalog, which the backend reads too, in place of vue-i18n's own syntax. Tags are
 * text, as ICU4J reads them.
 */
export function compileIcuMessage(message: unknown, { locale }: { locale: string }): CompiledMessage {
  const source = typeof message === 'string' ? message : '';
  const id = `${locale}\u0000${source}`;
  let format = formats.get(id);
  if (!format) {
    format = new IntlMessageFormat(source, locale, undefined, { ignoreTag: true });
    formats.set(id, format);
  }

  return ({ values }) => String(format.format(values as Record<string, string | number>));
}
