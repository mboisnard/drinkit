export type DateFormat = 'date' | 'dateLong' | 'dateTime' | 'time';

export type NumberFormat = 'decimal' | 'integer' | 'percent' | 'currency';

const DATE_FORMATS: Record<DateFormat, Intl.DateTimeFormatOptions> = {
  date: { dateStyle: 'medium' },
  dateLong: { dateStyle: 'long' },
  dateTime: { dateStyle: 'medium', timeStyle: 'short' },
  time: { timeStyle: 'short' },
};

const NUMBER_FORMATS: Record<NumberFormat, Intl.NumberFormatOptions> = {
  decimal: { maximumFractionDigits: 2 },
  integer: { maximumFractionDigits: 0 },
  percent: { style: 'percent' },
  currency: { style: 'currency' },
};

/** The currency an amount needs, such as `EUR`, and no other number. */
export type CurrencyOf<Format extends NumberFormat> = Format extends 'currency' ? [currency: string] : [];

/**
 * A date in the conventions of the language, such as `fr-FR`, in the time zone it is given: the server and the
 * browser render the same text, whatever zone their machine is in.
 */
export function formatDate(
  value: Date | number,
  format: DateFormat,
  { language, timeZone }: { language: string; timeZone: string },
): string {
  // eslint-disable-next-line no-restricted-syntax -- the one place that formats, from the named formats
  return new Intl.DateTimeFormat(language, { ...DATE_FORMATS[format], timeZone }).format(value);
}

export function formatNumber(
  value: number,
  format: NumberFormat,
  { language, currency }: { language: string; currency?: string },
): string {
  // eslint-disable-next-line no-restricted-syntax -- the one place that formats, from the named formats
  return new Intl.NumberFormat(language, { ...NUMBER_FORMATS[format], currency }).format(value);
}
