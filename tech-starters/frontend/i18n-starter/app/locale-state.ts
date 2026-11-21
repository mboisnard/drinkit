/** A key of useState where @nuxtjs/i18n keeps the locales: a reset of the app's state spares it, or they are lost. */
export function isLocaleState(key: string): boolean {
  return key.startsWith('i18n:');
}
