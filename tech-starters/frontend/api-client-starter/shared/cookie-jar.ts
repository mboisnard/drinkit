// The cookies of the backend's security starter, which adds the __Host- prefix outside the dev profile
const API_COOKIES = ['SESSION', 'XSRF-TOKEN', 'remember-me', 'visitor'];

/**
 * The cookies of one server render. Each call carries the cookies the backend set during the earlier calls, so a
 * rotated remember-me cookie rotates once, and the page sends the browser one `Set-Cookie` per name.
 */
export class CookieJar {
  readonly #values: Map<string, string>;
  readonly #setCookies = new Map<string, string>();

  /** Only the cookies of the API: the other cookies of the browser are none of the backend's business. */
  constructor(cookieHeader: string) {
    this.#values = new Map(cookieHeader.split(';').filter((pair) => pair.includes('=')).map(toNameAndValue)
      .filter(([name]) => API_COOKIES.includes(name.replace(/^__Host-/, ''))));
  }

  receive(setCookies: string[]): void {
    for (const setCookie of setCookies) {
      const [name, value] = toNameAndValue(setCookie);
      this.#setCookies.set(name, setCookie);
      if (isExpired(setCookie)) {
        this.#values.delete(name);
      } else {
        this.#values.set(name, value);
      }
    }
  }

  value(name: string): string | undefined {
    return this.#values.get(name);
  }

  cookieHeader(): string {
    return [...this.#values].map(([name, value]) => `${name}=${value}`).join('; ');
  }

  /** The `Set-Cookie` headers of the page: those it already has, unless the backend set the same name. */
  setCookies(pageSetCookies: string[]): string[] {
    const others = pageSetCookies.filter((setCookie) => !this.#setCookies.has(toNameAndValue(setCookie)[0]));

    return [...others, ...this.#setCookies.values()];
  }
}

function toNameAndValue(pair: string): [string, string] {
  const [name = '', ...value] = pair.split('=');

  return [name.trim(), value.join('=').split(';', 1)[0]?.trim() ?? ''];
}

// Max-Age wins over Expires, as in a browser
function isExpired(setCookie: string): boolean {
  const attributes = new Map(setCookie.split(';').slice(1).map((attribute) => {
    const [name, value] = toNameAndValue(attribute);

    return [name.toLowerCase(), value];
  }));
  const maxAge = attributes.get('max-age');
  const expires = attributes.get('expires');

  if (maxAge !== undefined) {
    return Number(maxAge) <= 0;
  }

  return expires !== undefined && Date.parse(expires) <= Date.now();
}
