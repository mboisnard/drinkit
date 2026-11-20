const APP_ORIGIN = 'http://app.invalid';

/**
 * The return URL a page received, kept only when it stays on the app's origin: the browser would follow any other
 * one, and a link to the sign-in page could then send a freshly signed-in user to another site.
 *
 * OWASP: ASVS 5.0 V3.7.2, Unvalidated Redirects and Forwards Cheat Sheet
 */
export function safeRedirect(value: unknown): string | undefined {
  if (typeof value !== 'string' || !value.startsWith('/')) {
    return undefined;
  }

  // The URL parser reads it as the browser would: `//`, a backslash or a tab may reach another origin
  const url = new URL(value, APP_ORIGIN);

  // A dot segment may normalise to `//other.site`, which the browser would follow as another origin
  if (url.origin !== APP_ORIGIN || url.pathname.startsWith('//')) {
    return undefined;
  }

  return `${url.pathname}${url.search}${url.hash}`;
}
