import { getRequestIP, getRequestProtocol } from 'h3';
import type { H3Event } from 'h3';
import { CookieJar } from './cookie-jar.ts';

export interface Client {
  ip: string | undefined;
  protocol: 'http' | 'https';
}

/**
 * `browser` is a call of the page relayed as is. `server-render` is a call the server makes for a page, whose own
 * headers describe the page, not the call.
 */
export type CallOrigin = 'browser' | 'server-render';

// From the connection, as the address: the X-Forwarded-* headers of a request come from the client
export function clientOf(event: H3Event): Client {
  const protocol = getRequestProtocol(event, { xForwardedProto: false });

  return { ip: getRequestIP(event), protocol: protocol === 'https' ? 'https' : 'http' };
}

const READ_BY_THE_API: Record<CallOrigin, string[]> = {
  'browser': ['accept', 'accept-language', 'content-type', 'if-none-match', 'user-agent', 'x-xsrf-token'],
  'server-render': ['accept-language', 'user-agent'],
};

/** The headers a call to the backend carries: only those the API reads, and where the client really is. */
export function relayHeaders(incoming: Headers, client: Client, origin: CallOrigin): Headers {
  const headers = new Headers();
  for (const [name, value] of incoming) {
    if (READ_BY_THE_API[origin].includes(name) || (origin === 'browser' && name.startsWith('sec-fetch-'))) {
      headers.set(name, value);
    }
  }

  const apiCookies = new CookieJar(incoming.get('cookie') ?? '').cookieHeader();
  if (apiCookies) {
    headers.set('cookie', apiCookies);
  }

  if (client.ip) {
    headers.set('x-forwarded-for', client.ip);
  }
  headers.set('x-forwarded-proto', client.protocol);

  return headers;
}
